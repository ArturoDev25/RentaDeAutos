package com.rentadeautos.modules.vehicle.dto;

import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;

import java.math.BigDecimal;

/**
 * Datos de un vehículo que devuelve la API.
 */
public record VehiculoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String placa,
        String vin,
        String marca,
        String modelo,
        Integer anio,
        String color,
        BigDecimal kilometraje,
        EstadoVehiculo estado
) {

    public static VehiculoResponse desde(Vehiculo vehiculo) {
        return new VehiculoResponse(
                vehiculo.getId(),
                vehiculo.getCategoria().getId(),
                vehiculo.getCategoria().getNombre(),
                vehiculo.getPlaca(),
                vehiculo.getVin(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAnio(),
                vehiculo.getColor(),
                vehiculo.getKilometraje(),
                vehiculo.getEstado()
        );
    }
}
