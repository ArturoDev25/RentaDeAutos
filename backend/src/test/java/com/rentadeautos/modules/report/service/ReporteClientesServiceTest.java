package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.ClientesResponse;
import com.rentadeautos.modules.report.repository.ClienteReporteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Comprueba el reporte de clientes activos e inactivos (S2-17).
 */
@ExtendWith(MockitoExtension.class)
class ReporteClientesServiceTest {

    @Mock
    private ClienteReporteRepository clienteReporteRepository;

    @InjectMocks
    private ReporteClientesService reporteClientesService;

    @Test
    @DisplayName("Genera el total sumando activos e inactivos")
    void generarSumaActivosEInactivos() {
        when(clienteReporteRepository.countByActivo(true)).thenReturn(3L);
        when(clienteReporteRepository.countByActivo(false)).thenReturn(2L);

        ClientesResponse respuesta = reporteClientesService.generar();

        assertEquals(5L, respuesta.totalClientes());
        assertEquals(3L, respuesta.activos());
        assertEquals(2L, respuesta.inactivos());
    }

    @Test
    @DisplayName("Sin clientes, el reporte responde en cero sin lanzar error")
    void generarSinClientesDevuelveCero() {
        when(clienteReporteRepository.countByActivo(true)).thenReturn(0L);
        when(clienteReporteRepository.countByActivo(false)).thenReturn(0L);

        ClientesResponse respuesta = reporteClientesService.generar();

        assertEquals(0L, respuesta.totalClientes());
    }
}
