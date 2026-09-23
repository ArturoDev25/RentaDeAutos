package com.rentadeautos.modules.vehicle.exception;

/**
 * Se lanza cuando se busca una categoría que no existe.
 */
public class CategoriaNoEncontradaException extends RuntimeException {

    public CategoriaNoEncontradaException() {
        super("Categoría no encontrada");
    }
}
