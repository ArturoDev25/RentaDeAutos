package com.rentadeautos.modules.reservation.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.reservation.controller.ReservacionController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReservacionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReservacionExceptionHandler {
    @ExceptionHandler(ReservacionException.class)
    public ResponseEntity<ApiResponse<Void>> manejar(ReservacionException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("La reservación no cumple las restricciones de la base de datos"));
    }
}
