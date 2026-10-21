package com.rentadeautos.modules.catalog.dto;

import java.math.BigDecimal;

/**
 * Datos públicos; no expone placa, VIN ni información del personal.
 * La foto no viaja en la lista: tieneImagen indica si puede pedirse en
 * /api/v1/catalogo/vehiculos/{id}/imagen.
 */
public record VehiculoCatalogoResponse(Long id, String marca, String modelo,
        Integer anio, String color, Long categoriaId, String categoria,
        BigDecimal tarifaDia, boolean tieneImagen) {
}
