package com.rentadeautos.modules.report.dto;

/**
 * Reporte de clientes: total, activos e inactivos.
 */
public record ClientesResponse(
        Long totalClientes,
        Long activos,
        Long inactivos
) {
}
