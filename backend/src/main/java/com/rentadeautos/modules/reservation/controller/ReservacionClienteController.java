package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.reservation.dto.ReservacionClienteResponse;
import com.rentadeautos.modules.reservation.dto.ReservacionClienteRequest;
import com.rentadeautos.modules.reservation.service.ReservacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.repository.ReservacionClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cliente/reservaciones")
public class ReservacionClienteController {
    private final ReservacionClienteRepository reservaciones;
    private final ReservacionService servicio;

    public ReservacionClienteController(ReservacionClienteRepository reservaciones,
            ReservacionService servicio) {
        this.reservaciones = reservaciones;
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservacionClienteResponse>> crear(
            @Valid @RequestBody
            ReservacionClienteRequest datos) {
        var r = servicio.crearDesdePortal(datos);
        var respuesta = new ReservacionClienteResponse(r.id(), r.vehiculoId(), r.fechaInicio(),
                r.fechaFin(), r.estado(), r.tarifaDia(), r.totalEstimado());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(respuesta));
    }

    @PatchMapping("/{id}/cancelar")
    public ApiResponse<ReservacionClienteResponse> cancelar(@PathVariable Long id) {
        return ApiResponse.success(servicio.cancelarDesdePortal(id));
    }

    @GetMapping
    public ApiResponse<List<ReservacionClienteResponse>> listar(Principal principal) {
        return ApiResponse.success(reservaciones.listar(clienteActual(principal)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservacionClienteResponse> obtener(Principal principal, @PathVariable Long id) {
        Long clienteId = clienteActual(principal);
        return ApiResponse.success(reservaciones.obtener(clienteId, id).orElseThrow(() ->
                new ReservacionException(HttpStatus.NOT_FOUND, "Reservación no encontrada")));
    }

    private Long clienteActual(Principal principal) {
        String identidad = principal == null ? null : principal.getName();
        if (identidad != null && identidad.matches("cliente:[1-9]\\d*")) {
            try {
                return Long.valueOf(identidad.substring("cliente:".length()));
            } catch (NumberFormatException ignorada) {
                // Una identidad fuera de rango tampoco corresponde a un cliente válido.
            }
        }
        throw new ReservacionException(HttpStatus.UNAUTHORIZED, "Identidad de cliente inválida");
    }
}
