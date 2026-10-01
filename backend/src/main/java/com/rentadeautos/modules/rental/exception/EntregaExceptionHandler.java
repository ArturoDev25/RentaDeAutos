package com.rentadeautos.modules.rental.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.rental.controller.EntregaController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EntregaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EntregaExceptionHandler {

    @ExceptionHandler(EntregaException.class)
    public ResponseEntity<ApiResponse<Void>> manejar(EntregaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    /** uq_entregas_reservacion: dos entregas simultáneas para la misma reservación. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("La entrega no cumple las restricciones de la base de datos "
                        + "(¿la reservación ya tiene una entrega?)"));
    }
}
