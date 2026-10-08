package com.rentadeautos.modules.rental.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.rental.dto.RentaActivaResponse;
import com.rentadeautos.modules.rental.service.RentaActivaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rentas/activas")
public class RentaActivaController {

    private final RentaActivaService servicio;

    public RentaActivaController(RentaActivaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ApiResponse<List<RentaActivaResponse>> listar() {
        return ApiResponse.success(servicio.listar());
    }
}
