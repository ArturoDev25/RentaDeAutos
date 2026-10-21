package com.rentadeautos.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.auth.dto.ClienteRegistroResponse;
import com.rentadeautos.modules.auth.exception.ClienteRegistroDuplicadoException;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.auth.service.ClienteRegistroService;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteRegistroController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClienteRegistroControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean ClienteRegistroService servicio;
    @MockBean JwtService jwt;
    @MockBean ClienteRepository clientes;
    @MockBean UsuarioAppRepository usuarios;
    private static final String RUTA = "/api/auth/cliente/registro";

    private Map<String, Object> datos() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Ana");
        datos.put("apellidos", "Prueba");
        datos.put("correo", "ana@example.test");
        datos.put("password", "Clave123!");
        datos.put("telefono", "8441234567");
        datos.put("numeroLicencia", "LIC-S4-02");
        datos.put("licenciaVencimiento", LocalDate.now().plusYears(1).toString());
        return datos;
    }

    @Test void registroEsPublicoYDevuelve201SinCredenciales() throws Exception {
        when(servicio.registrar(any())).thenReturn(
                new ClienteRegistroResponse(7L, "Ana", "Prueba", "ana@example.test", true));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(content().string(not(containsString("Clave123!"))));
        verify(servicio).registrar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "apellidos", "correo", "password", "telefono", "numeroLicencia", "licenciaVencimiento"})
    void campoObligatorioAusenteDevuelve400(String campo) throws Exception {
        var datos = datos();
        datos.remove(campo);
        rechazar(datos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "apellidos", "correo", "password", "telefono", "numeroLicencia"})
    void camposEnBlancoDevuelven400(String campo) throws Exception {
        var datos = datos();
        datos.put(campo, "   ");
        rechazar(datos);
    }

    @Test void correoInvalidoDevuelve400() throws Exception {
        var datos = datos();
        datos.put("correo", "sin-arroba");
        rechazar(datos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "a", ""})
    void passwordCortaDevuelve400(String password) throws Exception {
        var datos = datos();
        datos.put("password", password);
        rechazar(datos);
    }

    @Test void passwordLargaDevuelve400SinMostrarSuContenido() throws Exception {
        var datos = datos();
        String password = "z".repeat(73);
        datos.put("password", password);
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(password))));
        verifyNoInteractions(servicio);
    }

    @Test void limiteSeValidaTambienEnBytesUtf8() throws Exception {
        var datos = datos();
        datos.put("password", "á".repeat(37)); // 37 caracteres, 74 bytes.
        rechazar(datos);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void licenciaNoVigenteDevuelve400(int dias) throws Exception {
        var datos = datos();
        datos.put("licenciaVencimiento", LocalDate.now().plusDays(dias).toString());
        rechazar(datos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "apellidos", "correo", "telefono", "numeroLicencia", "direccion"})
    void longitudExcesivaDevuelve400(String campo) throws Exception {
        var datos = datos();
        datos.put(campo, "x".repeat(256));
        rechazar(datos);
    }

    @Test void jsonMalformadoDevuelve400() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test void duplicadoDevuelve409ConMensajeGenerico() throws Exception {
        when(servicio.registrar(any())).thenThrow(new ClienteRegistroDuplicadoException());
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se pudo completar el registro con los datos proporcionados."))
                .andExpect(content().string(not(containsString("ana@example.test"))))
                .andExpect(content().string(not(containsString("Clave123!"))));
    }

    @Test void altaDePersonalSigueProtegida() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test void noSeAbrenOtrosMetodosAnonimos() throws Exception {
        mvc.perform(delete(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    private void rechazar(Map<String, Object> datos) throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(servicio);
    }
}
