package com.rentadeautos.modules.report.dto;

/**
 * Una etiqueta (estado o categoría) con su cantidad de vehículos.
 */
public record ConteoResponse(
        String etiqueta,
        Long cantidad
) {
}
