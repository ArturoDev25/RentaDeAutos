package com.rentadeautos.modules.reservation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Renta iniciada: el total final y devolución real son nulos mientras no se cierre. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RentaClienteResponse(Long reservacionId, Long vehiculoId, String marca, String modelo,
        String estado, LocalDateTime fechaEntrega, LocalDateTime fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal, BigDecimal tarifaDia,
        BigDecimal totalEstimado, BigDecimal totalFinal,
        Integer anio, String categoria) {

    public RentaClienteResponse(Long reservacionId, Long vehiculoId, String marca, String modelo,
            String estado, LocalDateTime fechaEntrega, LocalDateTime fechaDevolucionPrevista,
            LocalDateTime fechaDevolucionReal, BigDecimal tarifaDia,
            BigDecimal totalEstimado, BigDecimal totalFinal) {
        this(reservacionId, vehiculoId, marca, modelo, estado, fechaEntrega,
                fechaDevolucionPrevista, fechaDevolucionReal, tarifaDia,
                totalEstimado, totalFinal, null, null);
    }
}
