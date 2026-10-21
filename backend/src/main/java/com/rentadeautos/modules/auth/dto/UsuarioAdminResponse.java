package com.rentadeautos.modules.auth.dto;

/**
 * Vista de un usuario para la administración de cuentas.
 * Incluye el estado activo para poder gestionarlo; nunca expone el password.
 */
public record UsuarioAdminResponse(
        Long id,
        String nombre,
        String correo,
        String rol,
        Boolean activo
) {
}
