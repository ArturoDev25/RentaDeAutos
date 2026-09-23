package com.rentadeautos.modules.vehicle.dto;

import com.rentadeautos.modules.vehicle.model.Categoria;

import java.math.BigDecimal;

/**
 * Datos de una categoría que devuelve la API.
 */
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal depositoBase,
        Boolean activo
) {

    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getDepositoBase(),
                categoria.getActivo()
        );
    }
}
