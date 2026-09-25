package com.rentadeautos.modules.client.exception;

public class ClienteDuplicadoException extends RuntimeException {

    public static final String CORREO_DUPLICADO = "Ya existe un cliente con ese correo";
    public static final String LICENCIA_DUPLICADA = "Ya existe un cliente con esa licencia";

    private ClienteDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public static ClienteDuplicadoException correo() {
        return new ClienteDuplicadoException(CORREO_DUPLICADO);
    }

    public static ClienteDuplicadoException licencia() {
        return new ClienteDuplicadoException(LICENCIA_DUPLICADA);
    }
}