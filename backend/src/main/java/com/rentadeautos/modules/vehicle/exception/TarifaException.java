package com.rentadeautos.modules.vehicle.exception;

import org.springframework.http.HttpStatus;

public class TarifaException extends RuntimeException {
    private final HttpStatus status;

    public TarifaException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
