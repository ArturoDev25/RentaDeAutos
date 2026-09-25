package com.rentadeautos.modules.vehicle.dto;

import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para cambiar el estado operativo de un vehículo.
 * Un valor fuera de la lista de EstadoVehiculo se rechaza con 400.
 */
public record EstadoVehiculoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoVehiculo estado

) {
}
