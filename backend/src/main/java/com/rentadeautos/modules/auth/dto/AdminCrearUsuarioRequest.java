package com.rentadeautos.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para que un ADMINISTRADOR cree una cuenta de personal
 * eligiendo el rol (a diferencia del registro normal, que siempre crea AGENTE).
 */
public record AdminCrearUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
