package com.rentadeautos.modules.vehicle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaRequest(
        @NotNull(message = "La categoría es obligatoria")
        @Positive(message = "La categoría debe ser un ID positivo") Long categoriaId,
        @NotNull(message = "El precio diario es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El precio diario debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El precio diario admite 8 enteros y 2 decimales")
        BigDecimal precioDia,
        @NotNull(message = "El cargo por atraso es obligatorio")
        @DecimalMin(value = "0", message = "El cargo por atraso no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El cargo por atraso admite 8 enteros y 2 decimales")
        BigDecimal cargoAtrasoDia,
        @NotNull(message = "La fecha inicial es obligatoria") LocalDate fechaInicio,
        LocalDate fechaFin) {
}
