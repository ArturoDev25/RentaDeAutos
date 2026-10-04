package com.rentadeautos.modules.devolucion.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio para el módulo de devoluciones (S3-10).
 * Transporta el código HTTP que debe devolver la API al cliente.
 */
public class DevolucionException extends RuntimeException {

    private final HttpStatus status;

    public DevolucionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
