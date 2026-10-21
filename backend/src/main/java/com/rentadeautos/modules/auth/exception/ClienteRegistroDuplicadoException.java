package com.rentadeautos.modules.auth.exception;

public class ClienteRegistroDuplicadoException extends RuntimeException {
    public ClienteRegistroDuplicadoException() {
        super("No se pudo completar el registro con los datos proporcionados.");
    }
}
