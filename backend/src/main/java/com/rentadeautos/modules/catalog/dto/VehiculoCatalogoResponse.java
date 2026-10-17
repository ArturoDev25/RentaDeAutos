package com.rentadeautos.modules.catalog.dto;

import java.math.BigDecimal;

/** Datos públicos; no expone placa, VIN ni información del personal. */
public record VehiculoCatalogoResponse(Long id, String marca, String modelo,
        Integer anio, String color, Long categoriaId, String categoria,
        BigDecimal tarifaDia) {
}
