package com.rentadeautos.modules.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos operativos de una reservación cuya entrega ya inició la renta. */
public record RentaActivaResponse(Long reservacionId, Long clienteId,
        String clienteNombre, String clienteTelefono, Long vehiculoId,
        String marca, String modelo, String placa, LocalDateTime fechaInicio,
        LocalDateTime fechaDevolucionPrevista, String estado,
        BigDecimal tarifaDia, BigDecimal totalEstimado) {
}
