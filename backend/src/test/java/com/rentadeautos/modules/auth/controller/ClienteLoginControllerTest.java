package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.auth.service.ClienteLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S4-03: la ruta es pública y responde 200 / 401 / 400 según el caso. */
@WebMvcTest(ClienteLoginController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClienteLoginControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ClienteLoginService servicio;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;

    private static final String DATOS = """
            {"correo":"ana@demo.local","password":"Clave-Segura-123"}
            """;

    @Test
    void credencialesValidasDevuelven200ConTokenSinNecesitarSesion() throws Exception {
        var respuesta = new LoginResponse("token-de-prueba", "Bearer", 1800,
                new UsuarioResponse(7L, "Ana Pérez", "ana@demo.local", "CLIENTE"));
        when(servicio.login(any())).thenReturn(Optional.of(respuesta));

        mvc.perform(post("/api/auth/cliente/login")
                        .contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token-de-prueba"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void credencialesInvalidasDevuelven401ConMensajeGenerico() throws Exception {
        when(servicio.login(any())).thenReturn(Optional.empty());

        mvc.perform(post("/api/auth/cliente/login")
                        .contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("incorrectos")));
    }

    @Test
    void correoConFormatoInvalidoSeRechazaSinInvocarServicio() throws Exception {
        mvc.perform(post("/api/auth/cliente/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("ana@demo.local", "no-es-un-correo")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void passwordVaciaSeRechazaSinInvocarServicio() throws Exception {
        mvc.perform(post("/api/auth/cliente/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("Clave-Segura-123", "")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
