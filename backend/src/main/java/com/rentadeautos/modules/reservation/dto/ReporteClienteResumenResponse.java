package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumen informativo de operaciones del cliente autenticado. */
public record ReporteClienteResumenResponse(String periodo, long totalRentas,
        BigDecimal gastoTotalEstimado, long diasRentados, BigDecimal costoPromedioDia,
        VariacionKpiResponse variacionPeriodoAnterior,
        List<SeriePeriodoItem> seriePorPeriodo,
        List<EstadoResumenItem> distribucionEstado,
        List<HistorialReporteItem> historial,
        String avisoImportes) {
}
