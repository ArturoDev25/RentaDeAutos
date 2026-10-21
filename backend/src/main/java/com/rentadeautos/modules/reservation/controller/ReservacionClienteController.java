package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.reservation.dto.ReservacionClienteResponse;
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

    public ReservacionClienteController(ReservacionClienteRepository reservaciones) {
        this.reservaciones = reservaciones;
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
