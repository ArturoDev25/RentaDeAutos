package com.rentadeautos.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los tokens JWT del sistema (S1-08).
 * El secreto proviene de la variable de entorno JWT_SECRET.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutosExpiracion;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-minutes}") long minutosExpiracion) {

        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosExpiracion = minutosExpiracion;
    }

    /**
     * Crea un token firmado para un usuario autenticado.
     */
    public String generarToken(String correo, String rol) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plusSeconds(minutosExpiracion * 60);

        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vence))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida la firma y la expiración de un token.
     * Devuelve vacío si el token es inválido, alterado o expiró.
     */
    public Optional<Claims> validarToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getSegundosExpiracion() {
        return minutosExpiracion * 60;
    }
}