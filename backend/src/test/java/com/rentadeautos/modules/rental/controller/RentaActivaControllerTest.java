package com.rentadeautos.modules.rental.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.rental.dto.RentaActivaResponse;
import com.rentadeautos.modules.rental.service.RentaActivaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentaActivaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RentaActivaControllerTest {

    private static final String RUTA = "/api/v1/rentas/activas";

    @Autowired MockMvc mvc;
    @MockBean RentaActivaService servicio;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void administradorConsultaDatosOperativosYFechaPrevista() throws Exception {
        RentaActivaResponse renta = new RentaActivaResponse(10L, 2L, "Ana Pérez",
                "8441234567", 3L, "Nissan", "Kicks", "DEM1234",
                LocalDateTime.of(2026, 10, 1, 10, 0),
                LocalDateTime.of(2026, 10, 4, 10, 0), "EN_CURSO",
                new BigDecimal("850.00"), new BigDecimal("2550.00"), true);
        when(servicio.listar()).thenReturn(List.of(renta));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reservacionId").value(10))
                .andExpect(jsonPath("$.data[0].clienteId").value(2))
                .andExpect(jsonPath("$.data[0].clienteNombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.data[0].clienteTelefono").value("8441234567"))
                .andExpect(jsonPath("$.data[0].vehiculoId").value(3))
                .andExpect(jsonPath("$.data[0].marca").value("Nissan"))
                .andExpect(jsonPath("$.data[0].modelo").value("Kicks"))
                .andExpect(jsonPath("$.data[0].placa").value("DEM1234"))
                .andExpect(jsonPath("$.data[0].fechaInicio").value("2026-10-01T10:00:00"))
                .andExpect(jsonPath("$.data[0].fechaDevolucionPrevista").value("2026-10-04T10:00:00"))
                .andExpect(jsonPath("$.data[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.data[0].retrasada").value(true))
                .andExpect(jsonPath("$.data[0].tarifaDia").value(850.0))
                .andExpect(jsonPath("$.data[0].totalEstimado").value(2550.0));
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR")
    void sinRentasActivasDevuelve200YListaVacia() throws Exception {
        when(servicio.listar()).thenReturn(List.of());
        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void sinAutenticacionDevuelve401() throws Exception {
        mvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGENTE", "SUPERVISOR", "AUDITOR"})
    void personalAutorizadoPuedeConsultar(String rol) throws Exception {
        when(servicio.listar()).thenReturn(List.of());
        mvc.perform(get(RUTA).with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "RENTERO"})
    void otrosRolesNoConsultanRentasActivas(String rol) throws Exception {
        mvc.perform(get(RUTA).with(user("usuario").roles(rol)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @WithMockUser(roles = "ADMINISTRADOR")
    void metodosDeEscrituraEstanBloqueados(String metodo) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), RUTA))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }
}
