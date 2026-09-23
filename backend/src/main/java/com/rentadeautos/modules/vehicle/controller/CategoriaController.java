package com.rentadeautos.modules.vehicle.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.dto.CategoriaRequest;
import com.rentadeautos.modules.vehicle.dto.CategoriaResponse;
import com.rentadeautos.modules.vehicle.dto.EstadoCategoriaRequest;
import com.rentadeautos.modules.vehicle.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de categorías de vehículos (S2-08).
 * No existe endpoint de eliminación: las categorías solo se desactivan.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** Lista las categorías; con ?activo=true|false se filtra por estado. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listar(
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(ApiResponse.success(categoriaService.listar(activo)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoriaService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(
            @Valid @RequestBody CategoriaRequest peticion) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoriaService.crear(peticion)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> editar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest peticion) {
        return ResponseEntity.ok(ApiResponse.success(categoriaService.editar(id, peticion)));
    }

    /** Activa o desactiva una categoría sin eliminarla. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<CategoriaResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoCategoriaRequest peticion) {
        return ResponseEntity.ok(
                ApiResponse.success(categoriaService.cambiarEstado(id, peticion.activo())));
    }
}
