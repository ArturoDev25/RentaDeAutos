package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.ReservacionesResponse;
import com.rentadeautos.modules.report.repository.ReservacionReporteRepository;
import com.rentadeautos.modules.report.repository.ReservacionReporteRepository.ConteoEstadoReservacion;
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
 * Comprueba el reporte de reservaciones por estado (S2-17).
 */
@ExtendWith(MockitoExtension.class)
class ReporteReservacionesServiceTest {

    @Mock
    private ReservacionReporteRepository reservacionReporteRepository;

    @InjectMocks
    private ReporteReservacionesService reporteReservacionesService;

    private record ConteoEstadoFalso(String estado, Long cantidad) implements ConteoEstadoReservacion {
        public String getEstado() { return estado; }
        public Long getCantidad() { return cantidad; }
    }

    @Test
    @DisplayName("Genera el reporte sumando las reservaciones por estado")
    void generarSumaLasReservacionesPorEstado() {
        when(reservacionReporteRepository.contarPorEstado()).thenReturn(List.of(
                new ConteoEstadoFalso("CONFIRMADA", 4L),
                new ConteoEstadoFalso("PENDIENTE", 1L)
        ));

        ReservacionesResponse respuesta = reporteReservacionesService.generar();

        assertEquals(5L, respuesta.totalReservaciones());
        assertEquals(2, respuesta.porEstado().size());
    }

    @Test
    @DisplayName("Sin reservaciones, el reporte responde en cero sin lanzar error")
    void generarSinReservacionesDevuelveCero() {
        when(reservacionReporteRepository.contarPorEstado()).thenReturn(List.of());

        ReservacionesResponse respuesta = reporteReservacionesService.generar();

        assertEquals(0L, respuesta.totalReservaciones());
        assertTrue(respuesta.porEstado().isEmpty());
    }
}
