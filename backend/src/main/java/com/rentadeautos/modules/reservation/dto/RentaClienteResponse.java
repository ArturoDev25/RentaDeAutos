package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Renta iniciada: el total final y devolución real son nulos mientras no se cierre. */
public record RentaClienteResponse(Long reservacionId, Long vehiculoId, String marca, String modelo,
        String estado, LocalDateTime fechaEntrega, LocalDateTime fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal, BigDecimal tarifaDia,
        BigDecimal totalEstimado, BigDecimal totalFinal) {
}
