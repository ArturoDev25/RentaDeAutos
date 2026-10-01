package com.rentadeautos.modules.rental.dto;

import com.rentadeautos.modules.rental.model.Entrega;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EntregaResponse(Long id, Long reservacionId, Long vehiculoId, Long entregadoPorId,
        LocalDateTime fechaEntrega, BigDecimal kilometrajeSalida, BigDecimal combustibleSalida,
        String condicionSalida, String observaciones,
        String estadoReservacion, String estadoVehiculo) {

    public static EntregaResponse desde(Entrega e, Long vehiculoId,
            String estadoReservacion, String estadoVehiculo) {
        return new EntregaResponse(e.getId(), e.getReservacionId(), vehiculoId,
                e.getEntregadoPorId(), e.getFechaEntrega(), e.getKilometrajeSalida(),
                e.getCombustibleSalida(), e.getCondicionSalida(), e.getObservaciones(),
                estadoReservacion, estadoVehiculo);
    }
}
