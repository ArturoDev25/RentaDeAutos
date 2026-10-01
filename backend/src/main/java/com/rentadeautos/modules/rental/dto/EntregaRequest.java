package com.rentadeautos.modules.rental.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos capturados al entregar el vehículo (S3-09).
 * Los límites coinciden con chk_entregas_valores y los tipos DECIMAL de la tabla.
 */
public record EntregaRequest(
        @NotNull(message = "es obligatoria")
        @Positive(message = "debe ser un identificador válido")
        Long reservacionId,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.0", message = "no puede ser negativo")
        @Digits(integer = 9, fraction = 1, message = "admite hasta 9 enteros y 1 decimal")
        BigDecimal kilometrajeSalida,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.0", message = "debe estar entre 0 y 100")
        @DecimalMax(value = "100.0", message = "debe estar entre 0 y 100")
        @Digits(integer = 3, fraction = 2, message = "admite hasta 2 decimales")
        BigDecimal combustibleSalida,

        @NotBlank(message = "es obligatoria")
        @Size(max = 2000, message = "admite hasta 2000 caracteres")
        String condicionSalida,

        @Size(max = 2000, message = "admite hasta 2000 caracteres")
        String observaciones
) {}
