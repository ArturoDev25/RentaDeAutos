package com.rentadeautos.modules.catalog.controller;

import com.rentadeautos.common.dto.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Mantiene los errores de filtros dentro del contrato del catálogo. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CatalogoController.class)
public class CatalogoExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> filtroInvalido(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(ApiResponse.error(error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> tipoInvalido(MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "El parámetro " + error.getName() + " debe contener un número válido"));
    }
}
