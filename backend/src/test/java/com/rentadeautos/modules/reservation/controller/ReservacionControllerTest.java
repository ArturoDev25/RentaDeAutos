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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            {"clienteId":2,"vehiculoId":3,"fechaInicio":"2026-10-01T10:00:00",
             "fechaFin":"2026-10-02T10:00:00"}
            """;

    @Test @WithMockUser(roles = "AGENTE")
    void agenteNoPuedeCrearEnSprint2() throws Exception {
        mvc.perform(post("/api/reservaciones").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposAntesDeInvocarServicio() throws Exception {
        mvc.perform(post("/api/reservaciones").contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS.replace("\"clienteId\":2", "\"clienteId\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(servicio);
    }
}
