package com.rentadeautos.modules.auth.dto;

/**
 * Identidad pública de un usuario autenticado.
 * Nunca incluye password_hash (RN-AUTH-08).
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rol
) {
}