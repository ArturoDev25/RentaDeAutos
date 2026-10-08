package com.rentadeautos.modules.devolucion.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.devolucion.controller.DevolucionController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones del módulo de devoluciones (S3-10).
 * Mapea DevolucionException a respuestas JSON limpias con el código HTTP
 * que porta la excepción.  El orden HIGHEST_PRECEDENCE asegura que este
 * handler tiene prioridad sobre cualquier handler genérico.
 */
@RestControllerAdvice(assignableTypes = DevolucionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevolucionExceptionHandler {

    @ExceptionHandler(DevolucionException.class)
    public ResponseEntity<ApiResponse<Void>> manejar(DevolucionException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * uq_devoluciones_entrega: dos devoluciones simultáneas para la misma entrega.
     * Ocurre cuando la verificación en servicio pasa, pero la BD rechaza el INSERT
     * por la restricción de unicidad.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "La devolución no cumple las restricciones de la base de datos "
                        + "(¿la entrega ya tiene una devolución registrada?)"));
    }
}
