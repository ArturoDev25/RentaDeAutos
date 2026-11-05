package com.rentadeautos.modules.client.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import com.rentadeautos.modules.client.dto.RechazoClienteRequest;
import com.rentadeautos.modules.client.service.ClienteAprobacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S5-09: aprobación y rechazo de cuentas registradas desde el portal.
 * Acceso: ADMINISTRADOR, AGENTE y SUPERVISOR (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteAprobacionController {

    private final ClienteAprobacionService servicio;

    public ClienteAprobacionController(ClienteAprobacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> pendientes() {
        return ResponseEntity.ok(ApiResponse.success(servicio.listarPendientes()));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<ApiResponse<ClienteResponse>> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(servicio.aprobar(id)));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<ApiResponse<ClienteResponse>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RechazoClienteRequest peticion) {
        String motivo = peticion == null ? null : peticion.motivo();
        return ResponseEntity.ok(ApiResponse.success(servicio.rechazar(id, motivo)));
    }
}
