package com.rentadeautos.modules.report.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.report.dto.ClientesResponse;
import com.rentadeautos.modules.report.dto.InventarioResponse;
import com.rentadeautos.modules.report.dto.ReservacionesResponse;
import com.rentadeautos.modules.report.service.ReporteClientesService;
import com.rentadeautos.modules.report.service.ReporteInventarioService;
import com.rentadeautos.modules.report.service.ReporteReservacionesService;
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
    private final ReporteClientesService reporteClientesService;
    private final ReporteReservacionesService reporteReservacionesService;

    public ReporteController(ReporteInventarioService reporteInventarioService,
                              ReporteClientesService reporteClientesService,
                              ReporteReservacionesService reporteReservacionesService) {
        this.reporteInventarioService = reporteInventarioService;
        this.reporteClientesService = reporteClientesService;
        this.reporteReservacionesService = reporteReservacionesService;
    }

    @GetMapping("/inventario")
    public ResponseEntity<ApiResponse<InventarioResponse>> inventario() {
        return ResponseEntity.ok(ApiResponse.success(reporteInventarioService.generar()));
    }

    @GetMapping("/clientes")
    public ResponseEntity<ApiResponse<ClientesResponse>> clientes() {
        return ResponseEntity.ok(ApiResponse.success(reporteClientesService.generar()));
    }

    @GetMapping("/reservaciones")
    public ResponseEntity<ApiResponse<ReservacionesResponse>> reservaciones() {
        return ResponseEntity.ok(ApiResponse.success(reporteReservacionesService.generar()));
    }
}
