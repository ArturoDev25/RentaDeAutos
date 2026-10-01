package com.rentadeautos.modules.rental.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.rental.dto.EntregaRequest;
import com.rentadeautos.modules.rental.dto.EntregaResponse;
import com.rentadeautos.modules.rental.service.EntregaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3-09 — Entrega de vehículo.
 * POST registra la entrega; GET consulta la entrega de una reservación.
 */
@RestController
@RequestMapping("/api/v1/entregas")
public class EntregaController {

    private final EntregaService servicio;

    public EntregaController(EntregaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EntregaResponse>> registrar(
            @Valid @RequestBody EntregaRequest datos, HttpServletRequest request) {
        EntregaResponse entrega = servicio.registrar(datos, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(entrega));
    }

    @GetMapping("/reservacion/{reservacionId}")
    public ApiResponse<EntregaResponse> obtenerPorReservacion(@PathVariable Long reservacionId) {
        return ApiResponse.success(servicio.obtenerPorReservacion(reservacionId));
    }
}
