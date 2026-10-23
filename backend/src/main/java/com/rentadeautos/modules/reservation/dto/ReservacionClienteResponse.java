package com.rentadeautos.modules.reservation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista del portal, sin identificadores internos del personal. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservacionClienteResponse(Long id, Long vehiculoId,
        LocalDateTime fechaInicio, LocalDateTime fechaFin, String estado,
        BigDecimal tarifaDia, BigDecimal totalEstimado,
        String marca, String modelo, Integer anio, String categoria,
        Boolean puedeCancelar) {

    public ReservacionClienteResponse(Long id, Long vehiculoId,
            LocalDateTime fechaInicio, LocalDateTime fechaFin, String estado,
            BigDecimal tarifaDia, BigDecimal totalEstimado) {
        this(id, vehiculoId, fechaInicio, fechaFin, estado, tarifaDia, totalEstimado,
                null, null, null, null, null);
    }
}
