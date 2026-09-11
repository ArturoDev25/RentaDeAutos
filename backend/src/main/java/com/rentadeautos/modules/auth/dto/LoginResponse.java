package com.rentadeautos.modules.auth.dto;

/**
 * Respuesta del endpoint de login: token e identidad del usuario.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse user
) {
}