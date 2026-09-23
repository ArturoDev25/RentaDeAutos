package com.rentadeautos.modules.vehicle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para crear o editar una categoría de vehículos.
 */
public record CategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede exceder 80 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String descripcion,

        @NotNull(message = "El depósito base es obligatorio")
        @DecimalMin(value = "0.00", message = "El depósito base no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El depósito base admite hasta 2 decimales")
        BigDecimal depositoBase

) {
}
