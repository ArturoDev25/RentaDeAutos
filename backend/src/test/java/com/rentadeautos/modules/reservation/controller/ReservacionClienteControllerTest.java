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
    @MockBean com.rentadeautos.modules.reservation.service.ReservacionService servicio;
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
    private String alta() {
        var inicio = LocalDateTime.now().plusDays(10).withNano(0);
        return "{\"vehiculoId\":3,\"fechaInicio\":\"" + inicio
                + "\",\"fechaFin\":\"" + inicio.plusDays(1) + "\","
                + "\"clienteId\":999,\"creadoPorId\":999,\"tarifaDia\":1,\"estado\":\"CONFIRMADA\"}";
    }

    @Test void postDevuelve201ConValoresDelServidor() throws Exception {
        var fecha = LocalDateTime.now().plusDays(10);
        var respuesta = new com.rentadeautos.modules.reservation.dto.ReservacionResponse(
                5L, 7L, 3L, null, fecha, fecha.plusDays(1), "PENDIENTE",
                new BigDecimal("850"), new BigDecimal("850"), null);
        when(servicio.crearDesdePortal(org.mockito.ArgumentMatchers.any())).thenReturn(respuesta);
        mvc.perform(post(RUTA).contentType("application/json").content(alta())
                .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.tarifaDia").value(850))
                .andExpect(jsonPath("$.data.creadoPorId").doesNotExist());
        var captor = org.mockito.ArgumentCaptor.forClass(
                com.rentadeautos.modules.reservation.dto.ReservacionClienteRequest.class);
        verify(servicio).crearDesdePortal(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3L, captor.getValue().vehiculoId());
    }

    @Test void postInvalidoDevuelve400SinInvocarServicio() throws Exception {
        mvc.perform(post(RUTA).contentType("application/json").content("{\"vehiculoId\":-1}")
                .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test void postSinSesionDevuelve401() throws Exception {
        mvc.perform(post(RUTA).contentType("application/json").content(alta()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR"})
    void personalNoCreaPorElPortal(String rol) throws Exception {
        mvc.perform(post(RUTA).contentType("application/json").content(alta())
                .with(user("personal").roles(rol))).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test void postTraslapadoDevuelve409() throws Exception {
        when(servicio.crearDesdePortal(org.mockito.ArgumentMatchers.any())).thenThrow(
                new com.rentadeautos.modules.reservation.exception.ReservacionException(
                        org.springframework.http.HttpStatus.CONFLICT, "El vehículo ya tiene una reservación para esas fechas"));
        mvc.perform(post(RUTA).contentType("application/json").content(alta())
                .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }

    @Test void patchCancelaPropiaYDevuelve200() throws Exception {
        var fecha = LocalDateTime.now().plusDays(10);
        when(servicio.cancelarDesdePortal(5L)).thenReturn(new ReservacionClienteResponse(
                5L, 3L, fecha, fecha.plusDays(1), "CANCELADA", new BigDecimal("850"), new BigDecimal("850")));
        mvc.perform(patch(RUTA + "/5/cancelar").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("CANCELADA"));
        verify(servicio).cancelarDesdePortal(5L);
    }

    @Test void patchAjenoDevuelve404() throws Exception {
        when(servicio.cancelarDesdePortal(5L)).thenThrow(
                new com.rentadeautos.modules.reservation.exception.ReservacionException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Reservación no encontrada"));
        mvc.perform(patch(RUTA + "/5/cancelar").with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }

    @Test void patchSinSesionDevuelve401() throws Exception {
        mvc.perform(patch(RUTA + "/5/cancelar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR"})
    void personalNoCancelaEnRutaDelCliente(String rol) throws Exception {
        mvc.perform(patch(RUTA + "/5/cancelar").with(user("personal").roles(rol)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

}
