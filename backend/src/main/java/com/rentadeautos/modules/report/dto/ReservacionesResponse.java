package com.rentadeautos.modules.report.dto;

import java.util.List;

/**
 * Reporte de reservaciones: total y agrupadas por estado.
 */
public record ReservacionesResponse(
        Long totalReservaciones,
        List<ConteoResponse> porEstado
) {
}
