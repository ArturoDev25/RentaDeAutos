package com.rentadeautos.modules.vehicle.exception;

/**
 * Se lanza cuando la placa o el VIN ya pertenecen a otro vehículo.
 * El mensaje indica cuál de los dos campos está repetido.
 */
public class VehiculoDuplicadoException extends RuntimeException {

    public static final String PLACA_DUPLICADA = "Ya existe un vehículo con esa placa";
    public static final String VIN_DUPLICADO = "Ya existe un vehículo con ese VIN";

    private VehiculoDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public static VehiculoDuplicadoException placa() {
        return new VehiculoDuplicadoException(PLACA_DUPLICADA);
    }

    public static VehiculoDuplicadoException vin() {
        return new VehiculoDuplicadoException(VIN_DUPLICADO);
    }
}
