package com.rentadeautos.modules.vehicle.exception;

/**
 * Se lanza cuando los datos del vehículo no cumplen una regla de negocio
 * que no se puede validar solo con anotaciones (año máximo, categoría).
 */
public class VehiculoInvalidoException extends RuntimeException {

    public VehiculoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
