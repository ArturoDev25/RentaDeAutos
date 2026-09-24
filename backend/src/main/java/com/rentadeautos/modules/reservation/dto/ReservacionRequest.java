package com.rentadeautos.modules.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ReservacionRequest(
        @NotNull @Positive Long clienteId,
        @NotNull @Positive Long vehiculoId,
        @NotNull LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin,
        @Size(max = 2000) String observaciones
) {}
