package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.modules.auth.security.*;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.reservation.repository.ReservacionClienteRepository;
import com.rentadeautos.modules.reservation.dto.ReservacionClienteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservacionClienteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReservacionClienteControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReservacionClienteRepository reservaciones;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;
    @MockBean ClienteRepository clientes;
    private static final String RUTA = "/api/v1/cliente/reservaciones";

    @Test void listaUsaIdentidadAunqueEnvienOtroClienteId() throws Exception {
        when(reservaciones.listar(7L)).thenReturn(List.of());
        mvc.perform(get(RUTA).param("cliente_id", "99").param("clienteId", "99")
                .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        verify(reservaciones).listar(7L);
        verifyNoMoreInteractions(reservaciones);
    }

    @Test void consultaPropiaNoExponePersonal() throws Exception {
        var fecha = LocalDateTime.of(2026, 11, 1, 10, 0);
        var respuesta = new ReservacionClienteResponse(5L, 3L, fecha, fecha.plusDays(1),
                "PENDIENTE", new BigDecimal("850"), new BigDecimal("850"));
        when(reservaciones.obtener(7L, 5L)).thenReturn(Optional.of(respuesta));
        mvc.perform(get(RUTA + "/5").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.creadoPorId").doesNotExist())
                .andExpect(jsonPath("$.data.observaciones").doesNotExist());
    }

    @Test void registroAjenoOInexistenteDevuelve404() throws Exception {
        when(reservaciones.obtener(7L, 99L)).thenReturn(Optional.empty());
        mvc.perform(get(RUTA + "/99").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        verify(reservaciones).obtener(7L, 99L);
    }

    @Test void sinSesionNoConsulta() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(reservaciones);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR"})
    void personalNoUsaRutasDeCliente(String rol) throws Exception {
        mvc.perform(get(RUTA).with(user("empleado").roles(rol))).andExpect(status().isForbidden());
        verifyNoInteractions(reservaciones);
    }

    @ParameterizedTest @ValueSource(strings = {"correo@demo.local", "cliente:0", "cliente:-1", "cliente:999999999999999999999"})
    void identidadInvalidaNoConsulta(String identidad) throws Exception {
        mvc.perform(get(RUTA).with(user(identidad).roles("CLIENTE")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reservaciones);
    }

    @Test void identificadorNoNumericoDevuelve400() throws Exception {
        mvc.perform(get(RUTA + "/abc").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservaciones);
    }

    @Test void clienteNoPuedeConsultarRutaDelPersonal() throws Exception {
        mvc.perform(get("/api/reservaciones").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservaciones);
    }
}
