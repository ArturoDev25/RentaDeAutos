package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista del portal, sin identificadores internos del personal. */
public record ReservacionClienteResponse(Long id, Long vehiculoId,
        LocalDateTime fechaInicio, LocalDateTime fechaFin, String estado,
        BigDecimal tarifaDia, BigDecimal totalEstimado) {
}
