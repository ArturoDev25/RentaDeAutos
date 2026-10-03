package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.reservation.service.ReservacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservacionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReservacionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReservacionService servicio;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;

    private static final String DATOS = """
            {"clienteId":2,"vehiculoId":3,"fechaInicio":"2030-01-01T10:00:00",
             "fechaFin":"2030-01-02T10:00:00"}
            """;

    // ---------- Validacion (se conserva de Sprint 2) ----------

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposAntesDeInvocarServicio() throws Exception {
        mvc.perform(post("/api/reservaciones").contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("\"clienteId\":2", "\"clienteId\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(servicio);
    }

    // ---------- S3-12: roles autorizados ----------

    @Test @WithMockUser(roles = "AGENTE")
    void agentePuedeCrearReservacion() throws Exception {
        mvc.perform(post("/api/reservaciones").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated());
        verify(servicio).crear(any());
    }

    @Test @WithMockUser(roles = "AGENTE")
    void agentePuedeConfirmarReservacion() throws Exception {
        mvc.perform(post("/api/reservaciones/8/confirmar"))
                .andExpect(status().isOk());
        verify(servicio).confirmar(8L);
    }

    @Test @WithMockUser(roles = "SUPERVISOR")
    void supervisorPuedeConfirmarReservacion() throws Exception {
        mvc.perform(post("/api/reservaciones/8/confirmar"))
                .andExpect(status().isOk());
        verify(servicio).confirmar(8L);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConfirmarReservacion() throws Exception {
        mvc.perform(post("/api/reservaciones/8/confirmar"))
                .andExpect(status().isOk());
        verify(servicio).confirmar(8L);
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorPuedeConsultarReservaciones() throws Exception {
        mvc.perform(get("/api/reservaciones"))
                .andExpect(status().isOk());
        verify(servicio).listar();
    }

    // ---------- S3-12: pruebas negativas (rechazo sin tocar datos) ----------

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoPuedeConfirmar() throws Exception {
        mvc.perform(post("/api/reservaciones/8/confirmar"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoPuedeCrear() throws Exception {
        mvc.perform(post("/api/reservaciones").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoPuedeEditar() throws Exception {
        mvc.perform(put("/api/reservaciones/8").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoPuedeCancelar() throws Exception {
        mvc.perform(patch("/api/reservaciones/8/cancelar"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void sinSesionNoPuedeConfirmar() throws Exception {
        mvc.perform(post("/api/reservaciones/8/confirmar"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }
}
