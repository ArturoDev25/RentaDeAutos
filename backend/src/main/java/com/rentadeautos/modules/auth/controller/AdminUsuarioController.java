package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.auth.dto.AdminCrearUsuarioRequest;
import com.rentadeautos.modules.auth.dto.EstadoUsuarioRequest;
import com.rentadeautos.modules.auth.dto.UsuarioAdminResponse;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * Lista todas las cuentas del personal (incluye el estado activo).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioAdminResponse>>> listar() {
        List<UsuarioAdminResponse> usuarios = usuarioRepository.findAll().stream()
                .map(authService::aResponseAdmin)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(usuarios));
    }

    /**
     * Crea una cuenta de personal con el rol indicado por el administrador.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @Valid @RequestBody AdminCrearUsuarioRequest peticion) {

        UsuarioResponse creado = authService.crearUsuarioConRol(
                peticion.nombre(), peticion.correo(), peticion.password(), peticion.rol());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(creado));
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