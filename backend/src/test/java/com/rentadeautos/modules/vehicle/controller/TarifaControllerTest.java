package com.rentadeautos.modules.vehicle.controller;

import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.vehicle.dto.TarifaResponse;
import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TarifaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TarifaControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private TarifaService tarifas;
    @MockBean private JwtService jwtService;
    @MockBean private UsuarioAppRepository usuarios;

    private TarifaResponse tarifa() {
        return new TarifaResponse(1L, 2L, "SUV", new BigDecimal("850.00"),
                BigDecimal.ZERO, LocalDate.of(2026, 10, 1), null, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR"})
    void rolesPermitidosConsultanListado(String rol) throws Exception {
        when(tarifas.listar()).thenReturn(List.of(tarifa()));
        mvc.perform(get("/api/v1/tarifas").with(user("operador").roles(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoriaId").value(2))
                .andExpect(jsonPath("$.data[0].precioDia").value(850));
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void consultarDetalleDevuelveDatos() throws Exception {
        when(tarifas.obtener(1L)).thenReturn(tarifa());
        mvc.perform(get("/api/v1/tarifas/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void inexistenteDevuelve404ConMensaje() throws Exception {
        when(tarifas.obtener(99L)).thenThrow(new TarifaNoEncontradaException());
        mvc.perform(get("/api/v1/tarifas/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tarifa no encontrada"));
    }

    @Test void sinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/tarifas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(tarifas);
    }

    @Test @WithMockUser(roles = "CLIENTE")
    void rolNoPermitidoDevuelve403() throws Exception {
        mvc.perform(get("/api/v1/tarifas")).andExpect(status().isForbidden());
        verifyNoInteractions(tarifas);
    }
}
