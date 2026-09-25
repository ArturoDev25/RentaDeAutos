package com.rentadeautos.modules.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150, message = "Los apellidos no pueden exceder 150 caracteres")
        String apellidos,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        String telefono,

        @NotBlank(message = "El número de licencia es obligatorio")
        @Size(max = 50, message = "El número de licencia no puede exceder 50 caracteres")
        String numeroLicencia,

        @NotNull(message = "La fecha de vencimiento de la licencia es obligatoria")
        @Future(message = "La licencia debe tener una fecha de vencimiento futura")
        LocalDate licenciaVencimiento,

        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
        String direccion
) {
}