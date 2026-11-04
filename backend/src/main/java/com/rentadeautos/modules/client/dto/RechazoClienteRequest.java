package com.rentadeautos.modules.client.dto;

import jakarta.validation.constraints.Size;

/**
 * S5-09: motivo opcional del rechazo. Solo se guarda en la bitácora de auditoría.
 */
public record RechazoClienteRequest(
        @Size(max = 255, message = "El motivo no puede exceder 255 caracteres") String motivo
) {}
