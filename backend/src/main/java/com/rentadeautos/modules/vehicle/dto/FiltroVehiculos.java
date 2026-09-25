package com.rentadeautos.modules.vehicle.dto;

import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;

/**
 * Criterios de búsqueda del listado de vehículos (S2-07).
 * Todos son opcionales; un criterio nulo no filtra.
 *
 * @param texto       texto libre que se busca en placa, VIN, marca y modelo
 * @param estado      estado operativo exacto
 * @param categoriaId categoría del vehículo
 * @param anioDesde   año mínimo (inclusive)
 * @param anioHasta   año máximo (inclusive)
 */
public record FiltroVehiculos(
        String texto,
        EstadoVehiculo estado,
        Long categoriaId,
        Integer anioDesde,
        Integer anioHasta
) {

    /** Filtro vacío: devuelve todos los vehículos. */
    public static FiltroVehiculos sinFiltros() {
        return new FiltroVehiculos(null, null, null, null, null);
    }
}
