package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de la tabla de historial del reporte del cliente. */
public record HistorialReporteItem(Long reservacionId, String folio, String vehiculo,
        String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin,
        long dias, BigDecimal totalEstimado, BigDecimal totalFinal, String estado) {
}
