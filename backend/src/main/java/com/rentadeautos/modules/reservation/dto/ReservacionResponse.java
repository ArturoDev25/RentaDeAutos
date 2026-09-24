package com.rentadeautos.modules.reservation.dto;

import com.rentadeautos.modules.reservation.model.Reservacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservacionResponse(Long id, Long clienteId, Long vehiculoId, Long creadoPorId,
        LocalDateTime fechaInicio, LocalDateTime fechaFin, String estado,
        BigDecimal tarifaDia, BigDecimal totalEstimado, String observaciones) {
    public static ReservacionResponse desde(Reservacion r) {
        return new ReservacionResponse(r.getId(), r.getClienteId(), r.getVehiculoId(),
                r.getCreadoPorId(), r.getFechaInicio(), r.getFechaFin(), r.getEstado(),
                r.getTarifaDia(), r.getTotalEstimado(), r.getObservaciones());
    }
}
