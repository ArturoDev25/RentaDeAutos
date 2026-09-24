package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.reservation.dto.ReservacionRequest;
import com.rentadeautos.modules.reservation.dto.ReservacionResponse;
import com.rentadeautos.modules.reservation.service.ReservacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservaciones")
public class ReservacionController {
    private final ReservacionService servicio;
    public ReservacionController(ReservacionService servicio) { this.servicio = servicio; }

    @GetMapping
    public ApiResponse<List<ReservacionResponse>> listar() {
        return ApiResponse.success(servicio.listar());
    }
    @GetMapping("/opciones")
    public ApiResponse<Map<String, Object>> opciones() {
        return ApiResponse.success(servicio.opciones());
    }
    @GetMapping("/{id}")
    public ApiResponse<ReservacionResponse> obtener(@PathVariable Long id) {
        return ApiResponse.success(servicio.obtener(id));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<ReservacionResponse>> crear(@Valid @RequestBody ReservacionRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(servicio.crear(datos)));
    }
    @PutMapping("/{id}")
    public ApiResponse<ReservacionResponse> editar(@PathVariable Long id,
            @Valid @RequestBody ReservacionRequest datos) {
        return ApiResponse.success(servicio.editar(id, datos));
    }
    @PatchMapping("/{id}/cancelar")
    public ApiResponse<ReservacionResponse> cancelar(@PathVariable Long id) {
        return ApiResponse.success(servicio.cancelar(id));
    }
}
