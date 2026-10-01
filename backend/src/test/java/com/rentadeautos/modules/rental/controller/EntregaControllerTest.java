package com.rentadeautos.modules.rental.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.rental.dto.EntregaResponse;
import com.rentadeautos.modules.rental.exception.EntregaException;
import com.rentadeautos.modules.rental.service.EntregaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EntregaControllerTest {

    @Autowired MockMvc mvc;
    @MockBean EntregaService servicio;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;

    private static final String DATOS = """
            {"reservacionId":10,"kilometrajeSalida":15020.5,"combustibleSalida":75.5,
             "condicionSalida":"Sin golpes","observaciones":"Entrega en sucursal"}
            """;

    private EntregaResponse respuesta() {
        return new EntregaResponse(99L, 10L, 3L, 7L, LocalDateTime.of(2026, 10, 1, 10, 0),
                new BigDecimal("15020.5"), new BigDecimal("75.50"), "Sin golpes",
                "Entrega en sucursal", "EN_CURSO", "RENTADO");
    }

    @Test @WithMockUser(roles = "AGENTE")
    void agenteRegistraEntregaYRecibe201() throws Exception {
        when(servicio.registrar(any(), any())).thenReturn(respuesta());
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.estadoReservacion").value("EN_CURSO"))
                .andExpect(jsonPath("$.data.estadoVehiculo").value("RENTADO"));
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoPuedeRegistrarEntrega() throws Exception {
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void combustibleFueraDeRangoSeRechazaSinInvocarServicio() throws Exception {
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("75.5", "120")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "SUPERVISOR")
    void condicionVaciaYKilometrajeNegativoSeRechazan() throws Exception {
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("\"Sin golpes\"", "\"  \"").replace("15020.5", "-1")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "AGENTE")
    void reglaDeNegocioDevuelveSuCodigo() throws Exception {
        when(servicio.registrar(any(), any())).thenThrow(new EntregaException(HttpStatus.CONFLICT,
                "Solo se puede entregar un vehículo de una reservación CONFIRMADA"));
        mvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorPuedeConsultarEntrega() throws Exception {
        when(servicio.obtenerPorReservacion(10L)).thenReturn(respuesta());
        mvc.perform(get("/api/v1/entregas/reservacion/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservacionId").value(10));
    }
}
