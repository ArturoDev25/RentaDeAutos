package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.InventarioResponse;
import com.rentadeautos.modules.report.repository.VehiculoReporteRepository;
import com.rentadeautos.modules.report.repository.VehiculoReporteRepository.ConteoCategoria;
import com.rentadeautos.modules.report.repository.VehiculoReporteRepository.ConteoEstado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Comprueba el reporte de inventario de vehículos (S2-17).
 */
@ExtendWith(MockitoExtension.class)
class ReporteInventarioServiceTest {

    @Mock
    private VehiculoReporteRepository vehiculoReporteRepository;

    @InjectMocks
    private ReporteInventarioService reporteInventarioService;

    private record ConteoEstadoFalso(String estado, Long cantidad) implements ConteoEstado {
        public String getEstado() { return estado; }
        public Long getCantidad() { return cantidad; }
    }

    private record ConteoCategoriaFalso(String categoria, Long cantidad) implements ConteoCategoria {
        public String getCategoria() { return categoria; }
        public Long getCantidad() { return cantidad; }
    }

    @Test
    @DisplayName("Genera el inventario sumando los vehículos por estado")
    void generarSumaLosVehiculosPorEstado() {
        when(vehiculoReporteRepository.contarPorEstado()).thenReturn(List.of(
                new ConteoEstadoFalso("DISPONIBLE", 3L),
                new ConteoEstadoFalso("RENTADO", 2L)
        ));
        when(vehiculoReporteRepository.contarPorCategoria()).thenReturn(List.of(
                new ConteoCategoriaFalso("SUV", 5L)
        ));

        InventarioResponse respuesta = reporteInventarioService.generar();

        assertEquals(5L, respuesta.totalVehiculos());
        assertEquals(2, respuesta.porEstado().size());
        assertEquals(1, respuesta.porCategoria().size());
    }

    @Test
    @DisplayName("Sin vehículos, el inventario responde en cero sin lanzar error")
    void generarSinVehiculosDevuelveCero() {
        when(vehiculoReporteRepository.contarPorEstado()).thenReturn(List.of());
        when(vehiculoReporteRepository.contarPorCategoria()).thenReturn(List.of());

        InventarioResponse respuesta = reporteInventarioService.generar();

        assertEquals(0L, respuesta.totalVehiculos());
        assertTrue(respuesta.porEstado().isEmpty());
        assertTrue(respuesta.porCategoria().isEmpty());
    }
}
