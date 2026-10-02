package com.rentadeautos.modules.devolucion.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.devolucion.dto.DevolucionRequest;
import com.rentadeautos.modules.devolucion.dto.DevolucionResponse;
import com.rentadeautos.modules.devolucion.service.DevolucionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3-10 — Devolución de vehículo.
 *
 * <p>POST /api/v1/devoluciones registra la devolución de un vehículo
 * previamente entregado y calcula el costo final de la renta.</p>
 *
 * <p>Roles habilitados: ADMINISTRADOR, SUPERVISOR, AGENTE.
 * AUDITOR obtiene 403 Forbidden.</p>
 */
@RestController
@RequestMapping("/api/v1/devoluciones")
public class DevolucionController {

    private final DevolucionService servicio;

    public DevolucionController(DevolucionService servicio) {
        this.servicio = servicio;
    }

    /**
     * Registra la devolución de un vehículo.
     *
     * @param datos   payload validado con Bean Validation
     * @param request contexto HTTP (para capturar la IP del actor)
     * @return 201 Created con el resumen financiero de la devolución
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','AGENTE')")
    public ResponseEntity<ApiResponse<DevolucionResponse>> registrar(
            @Valid @RequestBody DevolucionRequest datos,
            HttpServletRequest request) {

        DevolucionResponse respuesta = servicio.registrar(datos, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(respuesta));
    }
}
