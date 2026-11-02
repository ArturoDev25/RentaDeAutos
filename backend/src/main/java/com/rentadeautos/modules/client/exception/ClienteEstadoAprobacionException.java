package com.rentadeautos.modules.client.exception;

/**
 * S5-09: la cuenta no está en un estado que permita la operación solicitada
 * (por ejemplo, aprobar un cliente que ya fue revisado). Se responde 409.
 */
public class ClienteEstadoAprobacionException extends RuntimeException {

    public ClienteEstadoAprobacionException(String mensaje) {
        super(mensaje);
    }
}
