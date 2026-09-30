package com.rentadeautos.modules.vehicle.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.dto.TarifaResponse;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tarifas")
public class TarifaController {
    private final TarifaService tarifas;

    public TarifaController(TarifaService tarifas) {
        this.tarifas = tarifas;
    }

    @GetMapping
    public ApiResponse<List<TarifaResponse>> listar() {
        return ApiResponse.success(tarifas.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<TarifaResponse> obtener(@PathVariable Long id) {
        return ApiResponse.success(tarifas.obtener(id));
    }
}
