package com.rentadeautos.modules.catalog.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo/vehiculos")
public class CatalogoController {
    private final CatalogoRepository catalogo;

    public CatalogoController(CatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public ApiResponse<List<VehiculoCatalogoResponse>> listar() {
        return ApiResponse.success(catalogo.listar(LocalDateTime.now()));
    }
}
