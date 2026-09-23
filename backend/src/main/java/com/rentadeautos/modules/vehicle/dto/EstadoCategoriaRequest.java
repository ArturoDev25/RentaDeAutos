package com.rentadeautos.modules.vehicle.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para activar o desactivar una categoría sin eliminarla.
 */
public record EstadoCategoriaRequest(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}
