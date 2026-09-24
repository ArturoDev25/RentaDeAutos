package com.rentadeautos.modules.report.dto;

import java.util.List;

/**
 * Reporte de inventario: vehículos agrupados por estado y por categoría.
 */
public record InventarioResponse(
        Long totalVehiculos,
        List<ConteoResponse> porEstado,
        List<ConteoResponse> porCategoria
) {
}
