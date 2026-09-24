package com.rentadeautos.modules.vehicle.exception;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.controller.VehiculoController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte los errores del módulo de vehículos en respuestas HTTP.
 * Solo aplica a VehiculoController; el manejador global sigue atendiendo
 * el resto de la API (por ejemplo, los 400 de validación).
 */
@RestControllerAdvice(assignableTypes = VehiculoController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VehiculoExceptionHandler {

    @ExceptionHandler(VehiculoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoEncontrado(
            VehiculoNoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(VehiculoDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicado(
            VehiculoDuplicadoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(VehiculoInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalido(
            VehiculoInvalidoException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Un filtro o id con formato inválido, por ejemplo ?estado=VOLANDO
     * o /api/v1/vehiculos/abc, se responde con 400 en lugar de 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleParametroInvalido(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Valor inválido para el parámetro " + ex.getName()));
    }

    /**
     * Respaldo: la base rechazó el registro por una restricción UNIQUE
     * (por ejemplo, dos altas simultáneas con la misma placa).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegridad(
            DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Ya existe un vehículo con esa placa o VIN"));
    }
}
