package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;

/** Porcentajes de variación del periodo seleccionado frente al periodo inmediato anterior. */
public record VariacionKpiResponse(BigDecimal rentasPct, BigDecimal gastoPct,
        BigDecimal diasPct, BigDecimal costoPromedioDiaPct) {
}
