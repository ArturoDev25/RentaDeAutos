package com.rentadeautos.modules.reservation.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.reservation.dto.ReporteClienteResumenResponse;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.service.ReporteClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/cliente/reportes")
public class ReporteClienteController {
    private final ReporteClienteService servicio;

    public ReporteClienteController(ReporteClienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    public ApiResponse<ReporteClienteResumenResponse> resumen(
            Principal principal,
            @RequestParam(name = "periodo", defaultValue = "3M") String periodo) {
        Long clienteId = clienteActual(principal);
        return ApiResponse.success(servicio.generarResumen(clienteId, periodo));
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
