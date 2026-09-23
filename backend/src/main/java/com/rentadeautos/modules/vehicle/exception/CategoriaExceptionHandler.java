package com.rentadeautos.modules.vehicle.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.controller.CategoriaController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte los errores del catálogo de categorías en respuestas HTTP.
 * Solo aplica a CategoriaController; el manejador global sigue atendiendo
 * el resto de la API.
 */
@RestControllerAdvice(assignableTypes = CategoriaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CategoriaExceptionHandler {

    @ExceptionHandler(CategoriaNoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoEncontrada(
            CategoriaNoEncontradaException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CategoriaDuplicadaException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicada(
            CategoriaDuplicadaException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Respaldo: la base rechazó el nombre por la restricción UNIQUE
     * (por ejemplo, "Camión" y "Camion" son iguales para MySQL).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegridad(
            DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Ya existe una categoría con ese nombre"));
    }
}
