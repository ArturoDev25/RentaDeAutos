package com.rentadeautos.modules.vehicle.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.controller.TarifaController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TarifaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TarifaExceptionHandler {
    @ExceptionHandler(TarifaException.class)
    public ResponseEntity<ApiResponse<Void>> negocio(TarifaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TarifaNoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> noEncontrada(TarifaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }
}
