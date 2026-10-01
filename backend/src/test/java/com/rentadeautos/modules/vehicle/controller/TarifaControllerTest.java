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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import com.rentadeautos.modules.vehicle.exception.TarifaException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private String cuerpo(String precio, String cargo, String inicio) {
        return "{\"categoriaId\":2,\"precioDia\":" + precio
                + ",\"cargoAtrasoDia\":" + cargo + ",\"fechaInicio\":" + inicio + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "SUPERVISOR"})
    void altaValidaDevuelve201(String rol) throws Exception {
        when(tarifas.crear(any())).thenReturn(tarifa());
        mvc.perform(post("/api/v1/tarifas").with(user("operador").roles(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("850.00", "0", "\"2026-10-01\"")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "850.001", "100000000"})
    @WithMockUser(roles = "ADMINISTRADOR")
    void precioInvalidoDevuelve400(String precio) throws Exception {
        mvc.perform(post("/api/v1/tarifas").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(precio, "0", "\"2026-10-01\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(tarifas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "null", "1.001", "100000000"})
    @WithMockUser(roles = "ADMINISTRADOR")
    void cargoInvalidoDevuelve400(String cargo) throws Exception {
        mvc.perform(post("/api/v1/tarifas").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("850", cargo, "\"2026-10-01\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tarifas);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void fechaInicialObligatoria() throws Exception {
        mvc.perform(post("/api/v1/tarifas").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("850", "0", "null"))).andExpect(status().isBadRequest());
        verifyNoInteractions(tarifas);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void traslapeDevuelve409() throws Exception {
        when(tarifas.crear(any())).thenThrow(new TarifaException(HttpStatus.CONFLICT, "Vigencia superpuesta"));
        mvc.perform(post("/api/v1/tarifas").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("850", "0", "\"2026-10-01\"")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Vigencia superpuesta"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGENTE", "AUDITOR", "CLIENTE"})
    void rolesSinPermisoNoCrean(String rol) throws Exception {
        mvc.perform(post("/api/v1/tarifas").with(user("operador").roles(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("850", "0", "\"2026-10-01\""))).andExpect(status().isForbidden());
        verifyNoInteractions(tarifas);
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void editarDevuelve200() throws Exception {
        when(tarifas.editar(eq(1L), any())).thenReturn(tarifa());
        mvc.perform(put("/api/v1/tarifas/1").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("900", "0", "\"2026-10-01\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void editarInexistenteDevuelve404() throws Exception {
        when(tarifas.editar(eq(99L), any())).thenThrow(new TarifaNoEncontradaException());
        mvc.perform(put("/api/v1/tarifas/99").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("900", "0", "\"2026-10-01\"")))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void editarConTraslapeDevuelve409() throws Exception {
        when(tarifas.editar(eq(1L), any())).thenThrow(new TarifaException(HttpStatus.CONFLICT, "Traslape"));
        mvc.perform(put("/api/v1/tarifas/1").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("900", "0", "\"2026-10-01\"")))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void editarPrecioCeroDevuelve400() throws Exception {
        mvc.perform(put("/api/v1/tarifas/1").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("0", "0", "\"2026-10-01\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tarifas);
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void auditorNoEdita() throws Exception {
        mvc.perform(put("/api/v1/tarifas/1").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("900", "0", "\"2026-10-01\"")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tarifas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "SUPERVISOR"})
    void desactivarDevuelve200YEstadoInactivo(String rol) throws Exception {
        var inactiva = new TarifaResponse(1L, 2L, "SUV", new BigDecimal("850.00"),
                BigDecimal.ZERO, LocalDate.of(2026, 10, 1), null, false);
        when(tarifas.desactivar(1L)).thenReturn(inactiva);
        mvc.perform(patch("/api/v1/tarifas/1/desactivar").with(user("operador").roles(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void desactivarInexistenteDevuelve404() throws Exception {
        when(tarifas.desactivar(99L)).thenThrow(new TarifaNoEncontradaException());
        mvc.perform(patch("/api/v1/tarifas/99/desactivar"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGENTE", "AUDITOR", "CLIENTE"})
    void rolesNoPermitidosNoDesactivan(String rol) throws Exception {
        mvc.perform(patch("/api/v1/tarifas/1/desactivar").with(user("operador").roles(rol)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tarifas);
    }

    @Test void bajaSinSesionDevuelve401() throws Exception {
        mvc.perform(patch("/api/v1/tarifas/1/desactivar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(tarifas);
    }
}
