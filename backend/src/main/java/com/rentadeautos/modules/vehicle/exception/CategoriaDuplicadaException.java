package com.rentadeautos.modules.vehicle.exception;

/**
 * Se lanza cuando ya existe otra categoría con el mismo nombre.
 */
public class CategoriaDuplicadaException extends RuntimeException {

    public CategoriaDuplicadaException() {
        super("Ya existe una categoría con ese nombre");
    }
}
