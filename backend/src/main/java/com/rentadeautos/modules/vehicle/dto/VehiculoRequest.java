package com.rentadeautos.modules.vehicle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para registrar o editar un vehículo.
 * No incluye el estado: un vehículo nuevo siempre inicia en DISPONIBLE
 * y el estado solo se cambia con PATCH /api/v1/vehiculos/{id}/estado.
 */
public record VehiculoRequest(

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9-]{3,13}[A-Za-z0-9]\\s*$",
                message = "La placa debe tener de 5 a 15 letras, números o guiones")
        String placa,

        @NotBlank(message = "El VIN es obligatorio")
        @Pattern(regexp = "^\\s*[A-HJ-NPR-Za-hj-npr-z0-9]{17}\\s*$",
                message = "El VIN debe tener 17 letras o números, sin I, O ni Q")
        String vin,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 80, message = "La marca no puede exceder 80 caracteres")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 80, message = "El modelo no puede exceder 80 caracteres")
        String modelo,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 2000, message = "El año no puede ser anterior a 2000")
        Integer anio,

        @Size(max = 40, message = "El color no puede exceder 40 caracteres")
        String color,

        @NotNull(message = "El kilometraje es obligatorio")
        @DecimalMin(value = "0.0", message = "El kilometraje no puede ser negativo")
        @Digits(integer = 9, fraction = 1, message = "El kilometraje admite hasta 1 decimal")
        BigDecimal kilometraje

) {
}
