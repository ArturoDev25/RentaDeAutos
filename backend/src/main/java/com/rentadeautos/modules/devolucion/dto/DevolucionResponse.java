package com.rentadeautos.modules.devolucion.dto;

import com.rentadeautos.modules.devolucion.model.Devolucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta JSON de registro de devolución (S3-10).
 * Incluye los datos de auditoría de costo y los estados resultantes
 * de la reservación y el vehículo.
 */
public record DevolucionResponse(
        Long id,
        Long entregaId,
        Long reservacionId,
        String vehiculoPlaca,
        LocalDateTime fechaDevolucion,
        BigDecimal kilometrajeEntrada,
        BigDecimal combustibleEntrada,
        String condicionEntrada,
        /** Subtotal base = tarifa_dia × días reales transcurridos. */
        BigDecimal subtotal,
        /** Recargo por devolución después de la fecha fin pactada. */
        BigDecimal cargoAtraso,
        /** Cargos por daños o faltantes declarados. */
        BigDecimal cargoDanos,
        /** Monto definitivo = subtotal + cargoAtraso + cargoDanos. */
        BigDecimal totalFinal,
        String estadoVehiculo,
        String estadoReservacion
) {

    /**
     * Constructor de conveniencia que combina la entidad guardada con los
     * datos de contexto que el servicio calcula fuera de la entidad.
     */
    public static DevolucionResponse desde(
            Devolucion d,
            Long reservacionId,
            String vehiculoPlaca,
            BigDecimal subtotal,
            String estadoVehiculo,
            String estadoReservacion) {

        return new DevolucionResponse(
                d.getId(),
                d.getEntregaId(),
                reservacionId,
                vehiculoPlaca,
                d.getFechaDevolucion(),
                d.getKilometrajeEntrada(),
                d.getCombustibleEntrada(),
                d.getCondicionEntrada(),
                subtotal,
                d.getCargoAtraso(),
                d.getCargoDanos(),
                d.getTotalFinal(),
                estadoVehiculo,
                estadoReservacion);
    }
}
