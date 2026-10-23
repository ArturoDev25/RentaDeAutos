package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.reservation.dto.ReporteClienteResumenResponse;
import com.rentadeautos.modules.reservation.dto.VariacionKpiResponse;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.service.ReporteClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteClienteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReporteClienteControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReporteClienteService servicio;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;
    @MockBean ClienteRepository clientes;

    private static final String RUTA = "/api/v1/cliente/reportes/resumen";

    @Test
    void usaIdentidadDelTokenAunqueEnvienOtroClienteId() throws Exception {
        var respuesta = new ReporteClienteResumenResponse(
                "3M", 2L, new BigDecimal("2500.00"), 3L, new BigDecimal("833.33"),
                new VariacionKpiResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), ReporteClienteService.AVISO_IMPORTES
        );
        when(servicio.generarResumen(7L, "3M")).thenReturn(respuesta);

        mvc.perform(get(RUTA).param("clienteId", "99").param("cliente_id", "99")
                        .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodo").value("3M"))
                .andExpect(jsonPath("$.data.totalRentas").value(2))
                .andExpect(jsonPath("$.data.avisoImportes").value(ReporteClienteService.AVISO_IMPORTES));

        verify(servicio).generarResumen(7L, "3M");
        verifyNoMoreInteractions(servicio);
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR"})
    void personalInternoRecibe403EnReportesDeCliente(String rol) throws Exception {
        mvc.perform(get(RUTA).with(user("empleado").roles(rol)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"correo@demo.local", "cliente:0", "cliente:-1", "cliente:999999999999999999999"})
    void identidadInvalidaDevuelve401(String identidad) throws Exception {
        mvc.perform(get(RUTA).with(user(identidad).roles("CLIENTE")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void noPermiteEscrituraEnReportesDeCliente(String metodo) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), RUTA)
                        .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void periodoInvalidoDevuelve400() throws Exception {
        when(servicio.generarResumen(7L, "99D")).thenThrow(
                new ReservacionException(HttpStatus.BAD_REQUEST, "Periodo inválido"));
        mvc.perform(get(RUTA).param("periodo", "99D")
                        .with(user("cliente:7").roles("CLIENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
