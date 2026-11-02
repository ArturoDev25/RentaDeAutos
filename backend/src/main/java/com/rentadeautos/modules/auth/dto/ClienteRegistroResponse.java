package com.rentadeautos.modules.auth.dto;

import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;

/**
 * S5-09: la cuenta nace inactiva y con estado PENDIENTE hasta que el personal la revise.
 */
public record ClienteRegistroResponse(
        Long id, String nombre, String apellidos, String correo, boolean activo,
        EstadoAprobacionCliente estadoAprobacion
) {}
