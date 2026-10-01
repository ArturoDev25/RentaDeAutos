package com.rentadeautos.modules.rental.exception;

import org.springframework.http.HttpStatus;

public class EntregaException extends RuntimeException {
    private final HttpStatus status;

    public EntregaException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
