package com.rentadeautos.modules.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para activar o desactivar un usuario sin eliminarlo.
 */
public record EstadoUsuarioRequest(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}