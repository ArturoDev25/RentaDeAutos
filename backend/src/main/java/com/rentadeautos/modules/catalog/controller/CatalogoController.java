package com.rentadeautos.modules.catalog.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

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
    public ApiResponse<List<VehiculoCatalogoResponse>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax) {
        if (categoriaId != null && categoriaId <= 0) {
            throw new IllegalArgumentException("categoriaId debe ser positivo");
        }
        validarPrecio(precioMin, "precioMin");
        validarPrecio(precioMax, "precioMax");
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new IllegalArgumentException("precioMin no puede ser mayor que precioMax");
        }
        return ApiResponse.success(catalogo.listar(LocalDateTime.now(), categoriaId, precioMin, precioMax));
    }

    private void validarPrecio(BigDecimal precio, String campo) {
        if (precio != null && (precio.signum() < 0
                || precio.compareTo(new BigDecimal("99999999.99")) > 0
                || precio.stripTrailingZeros().scale() > 2)) {
            throw new IllegalArgumentException(campo
                    + " debe estar entre 0 y 99999999.99 y tener como máximo dos decimales");
        }
    }
}
