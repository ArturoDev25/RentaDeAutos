package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.auth.dto.EstadoUsuarioRequest;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones administrativas relacionadas con los usuarios.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioAppRepository usuarioRepository;
    private final AuthService authService;

    public AdminUsuarioController(
            UsuarioAppRepository usuarioRepository,
            AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    /**
     * Activa o desactiva un usuario sin eliminarlo de la base de datos.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoUsuarioRequest peticion) {

        UsuarioApp usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Usuario no encontrado"));
        }

        usuario.setActivo(peticion.activo());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(
                ApiResponse.success(authService.aResponse(usuario))
        );
    }
}