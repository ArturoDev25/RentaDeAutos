package com.rentadeautos.modules.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

/** El portal no decide cliente, creador, estado, tarifa ni total. */
public record ReservacionClienteRequest(
        @NotNull @Positive Long vehiculoId,
        @NotNull @Future LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin,
        @Size(max = 2000) String observaciones) {
}
