package com.rentadeautos.modules.client.exception;

public class ClienteInvalidoException extends RuntimeException {

    public ClienteInvalidoException(String mensaje) {
        super(mensaje);
    }
}