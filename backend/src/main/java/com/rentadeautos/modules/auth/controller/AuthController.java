package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación (S1-08).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioAppRepository usuarioRepository;

    public AuthController(AuthService authService,
                          UsuarioAppRepository usuarioRepository) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Inicia sesión y devuelve un token con la identidad del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest peticion) {

        LoginResponse respuesta = authService.login(peticion);
        return ResponseEntity.ok(ApiResponse.success(respuesta));
    }

    /**
     * Devuelve la identidad del usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me(Authentication autenticacion) {
        String correo = autenticacion.getName();

        UsuarioApp usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado inexistente"));

        return ResponseEntity.ok(ApiResponse.success(authService.aResponse(usuario)));
    }

    /**
     * Cierra la sesión. El frontend debe descartar el token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}