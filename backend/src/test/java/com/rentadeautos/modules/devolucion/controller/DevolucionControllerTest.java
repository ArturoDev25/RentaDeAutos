package com.rentadeautos.modules.devolucion.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.devolucion.dto.DevolucionResponse;
import com.rentadeautos.modules.devolucion.exception.DevolucionException;
import com.rentadeautos.modules.devolucion.exception.DevolucionExceptionHandler;
import com.rentadeautos.modules.devolucion.service.DevolucionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de capa web para {@link DevolucionController} (S3-10).
 * Usa @WebMvcTest para levantar solo la capa MVC con la seguridad real.
 */
@WebMvcTest({DevolucionController.class, DevolucionExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DevolucionControllerTest {

    @Autowired MockMvc mvc;

    @MockBean DevolucionService servicio;
    @MockBean JwtService        jwt;
    @MockBean UsuarioAppRepository usuarios;

    /** Payload JSON mínimo válido para una devolución. */
    private static final String DATOS_VALIDOS = """
            {
              "entregaId": 50,
              "kilometrajeEntrada": 15500.5,
              "combustibleEntrada": 75.00,
              "condicionEntrada": "Sin golpes, estado general bueno",
              "cargoDanos": 0,
              "observaciones": "Devolución en sucursal norte",
              "requiereMantenimiento": false
            }
            """;

    private DevolucionResponse respuestaOk() {
        return new DevolucionResponse(
                200L, 50L, 10L, "ABC-123",
                LocalDateTime.of(2026, 10, 2, 14, 30),
                new BigDecimal("15500.5"),
                new BigDecimal("75.00"),
                "Sin golpes, estado general bueno",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                "DISPONIBLE",
                "FINALIZADA");
    }

    // ── Caso exitoso ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("POST exitoso retorna 201 Created con datos correctos")
    void agenteRegistraDevolucionYRecibe201() throws Exception {
        when(servicio.registrar(any(), any())).thenReturn(respuestaOk());

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.estadoReservacion").value("FINALIZADA"))
                .andExpect(jsonPath("$.data.estadoVehiculo").value("DISPONIBLE"))
                .andExpect(jsonPath("$.data.totalFinal").value(1000.00));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("POST exitoso también funciona para rol ADMINISTRADOR")
    void administradorRegistraDevolucionYRecibe201() throws Exception {
        when(servicio.registrar(any(), any())).thenReturn(respuestaOk());

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("POST exitoso también funciona para rol SUPERVISOR")
    void supervisorRegistraDevolucionYRecibe201() throws Exception {
        when(servicio.registrar(any(), any())).thenReturn(respuestaOk());

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isCreated());
    }

    // ── Validación Bean Validation ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("POST con kilometraje negativo retorna 400 Bad Request sin invocar servicio")
    void kilometrajeNegativoDevuelve400() throws Exception {
        String payload = DATOS_VALIDOS.replace("15500.5", "-1");

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(servicio);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("POST con combustible fuera de rango retorna 400 Bad Request")
    void combustibleFueraDeRangoDevuelve400() throws Exception {
        String payload = DATOS_VALIDOS.replace("75.00", "150.00");

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("POST con condicionEntrada vacía retorna 400 Bad Request")
    void condicionVaciaDevuelve400() throws Exception {
        String payload = DATOS_VALIDOS.replace(
                "Sin golpes, estado general bueno", "   ");

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    // ── Regla de negocio RN-08 ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("POST con kilometraje inconsistente retorna 400 Bad Request (RN-08)")
    void kilometrajeInconsistenteDevuelve400() throws Exception {
        when(servicio.registrar(any(), any()))
                .thenThrow(new DevolucionException(HttpStatus.BAD_REQUEST,
                        "RN-08: el kilometraje de entrada (14999.0) no puede ser menor al de salida (15000.0)"));

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("RN-08")));
    }

    // ── Control de acceso ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("Rol AUDITOR retorna 403 Forbidden")
    void auditorNoPuedeRegistrarDevolucion() throws Exception {
        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Sin token retorna 401 Unauthorized")
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    // ── Excepciones de negocio mapeadas ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Doble devolución retorna 409 Conflict")
    void dobleDevolucuonDevuelve409() throws Exception {
        when(servicio.registrar(any(), any()))
                .thenThrow(new DevolucionException(HttpStatus.CONFLICT,
                        "La entrega 50 ya tiene una devolución registrada"));

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Entrega no encontrada retorna 404 Not Found")
    void entregaNoEncontradaDevuelve404() throws Exception {
        when(servicio.registrar(any(), any()))
                .thenThrow(new DevolucionException(HttpStatus.NOT_FOUND,
                        "Entrega no encontrada: 50"));

        mvc.perform(post("/api/v1/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS_VALIDOS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
