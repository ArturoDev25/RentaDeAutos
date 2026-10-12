package com.rentadeautos.modules.vehicle.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.dto.CategoriaResponse;
import com.rentadeautos.modules.vehicle.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de categorías para el personal autorizado (S3).
 *
 * El catálogo de administración sigue en {@code /api/categorias} y es exclusivo
 * del Administrador. Aquí se expone ÚNICAMENTE la lectura bajo la ruta versionada
 * {@code /api/v1/categorias}, que SecurityConfig abre a los cuatro roles
 * (Administrador, Agente, Supervisor y Auditor). De este modo, por ejemplo, el
 * Supervisor puede consultar las categorías necesarias para dar de alta una
 * tarifa sin obtener acceso de escritura al catálogo. No se definen
 * POST/PUT/PATCH/DELETE: la gestión de categorías permanece solo en el
 * controlador de administración.
 */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaConsultaController {

    private final CategoriaService categoriaService;

    public CategoriaConsultaController(CategoriaService categoriaService) {
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
}
