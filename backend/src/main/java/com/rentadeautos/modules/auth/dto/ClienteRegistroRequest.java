package com.rentadeautos.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

public record ClienteRegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 150) String apellidos,
        @NotBlank @Email @Size(max = 150) String correo,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(min = 6, max = 72, message = "debe tener entre 6 y 72 caracteres")
        String password,
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(max = 50) String numeroLicencia,
        @NotNull @Future(message = "debe ser posterior a hoy") LocalDate licenciaVencimiento,
        @Size(max = 255) String direccion
) {
    public ClienteRegistroRequest {
        nombre = limpiar(nombre);
        apellidos = limpiar(apellidos);
        correo = correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
        telefono = limpiar(telefono);
        numeroLicencia = numeroLicencia == null ? null : numeroLicencia.trim().toUpperCase(Locale.ROOT);
        direccion = limpiar(direccion);
        // No recortar ni normalizar la contraseña.
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    @JsonIgnore
    @AssertTrue(message = "la contraseña no debe superar 72 bytes en UTF-8")
    public boolean isPasswordDentroLimite() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "ClienteRegistroRequest[datos omitidos]";
    }
}
