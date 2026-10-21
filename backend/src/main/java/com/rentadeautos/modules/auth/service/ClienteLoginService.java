package com.rentadeautos.modules.auth.service;

import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * S4-03: inicio de sesión del cliente del portal.
 *
 * Las credenciales viven en la tabla clientes (no en usuarios_app). Devuelve
 * vacío ante CUALQUIER fallo (correo inexistente, contraseña incorrecta, cuenta
 * inactiva o cliente sin cuenta de portal) para que el controlador responda
 * siempre lo mismo y no revele si un correo está registrado.
 */
@Service
public class ClienteLoginService {

    private static final Logger log = LoggerFactory.getLogger(ClienteLoginService.class);

    /** Rol que lleva el token de los clientes del portal. */
    public static final String ROL_CLIENTE = "CLIENTE";

    /**
     * Prefijo del "subject" del token. No es un correo a propósito: el filtro JWT
     * del personal busca el subject como correo en usuarios_app, y así un token
     * de cliente jamás puede confundirse con el de un usuario del personal.
     */
    public static final String PREFIJO_SUJETO = "cliente:";

    private final ClienteRepository clientes;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String hashSenuelo;

    public ClienteLoginService(ClienteRepository clientes,
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService) {
        this.clientes = clientes;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Hash de una contraseña que nadie conoce: se compara aunque el cliente no
        // exista, para que el tiempo de respuesta no delate qué correos existen.
        this.hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public Optional<LoginResponse> login(LoginRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        Cliente cliente = clientes.findByCorreoIgnoreCase(correo).orElse(null);
        String hash = (cliente != null) ? cliente.getPasswordHash() : null;

        boolean coincide = passwordEncoder.matches(
                peticion.password(), hash != null ? hash : hashSenuelo);

        boolean puedeEntrar = cliente != null
                && hash != null
                && Boolean.TRUE.equals(cliente.getActivo())
                && coincide;

        if (!puedeEntrar) {
            log.warn("Intento de acceso de cliente rechazado");
            return Optional.empty();
        }

        String token = jwtService.generarToken(PREFIJO_SUJETO + cliente.getId(), ROL_CLIENTE);
        log.info("Acceso exitoso del cliente {}", cliente.getId());

        String nombre = cliente.getNombre() + " " + cliente.getApellidos();
        return Optional.of(new LoginResponse(
                token,
                "Bearer",
                jwtService.getSegundosExpiracion(),
                new UsuarioResponse(cliente.getId(), nombre, cliente.getCorreo(), ROL_CLIENTE)));
    }
}
