package com.rentadeautos.modules.reservation.exception;

import org.springframework.http.HttpStatus;

public class ReservacionException extends RuntimeException {
    private final HttpStatus status;
    public ReservacionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
