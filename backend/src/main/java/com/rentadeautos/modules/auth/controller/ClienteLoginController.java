package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.service.ClienteLoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * S4-03: inicio de sesión del cliente del portal.
 */
@RestController
@RequestMapping("/api/auth/cliente")
public class ClienteLoginController {

    /** Mensaje único para todo fallo: no revela si el correo está registrado. */
    static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos";

    private final ClienteLoginService servicio;

    public ClienteLoginController(ClienteLoginService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest peticion) {
        Optional<LoginResponse> respuesta = servicio.login(peticion);
        if (respuesta.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(MENSAJE_CREDENCIALES));
        }
        return ResponseEntity.ok(ApiResponse.success(respuesta.get()));
    }
}
