package com.rentadeautos.modules.report.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.report.dto.InventarioResponse;
import com.rentadeautos.modules.report.service.ReporteInventarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/**
 * Reportes de solo lectura del panel de Administrador (S2-17).
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteInventarioService reporteInventarioService;

    public ReporteController(ReporteInventarioService reporteInventarioService) {
        this.reporteInventarioService = reporteInventarioService;
    }

    @GetMapping("/inventario")
    public ResponseEntity<ApiResponse<InventarioResponse>> inventario() {
        return ResponseEntity.ok(ApiResponse.success(reporteInventarioService.generar()));
    }
}
