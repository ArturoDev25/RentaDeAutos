package com.rentadeautos.modules.client.exception;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException() {
        super("Cliente no encontrado");
    }
}