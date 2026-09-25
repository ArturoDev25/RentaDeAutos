package com.rentadeautos.modules.vehicle.exception;

/**
 * Se lanza cuando se busca un vehículo que no existe.
 */
public class VehiculoNoEncontradoException extends RuntimeException {

    public VehiculoNoEncontradoException() {
        super("Vehículo no encontrado");
    }
}
