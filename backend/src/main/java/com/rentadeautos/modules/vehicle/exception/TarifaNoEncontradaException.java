package com.rentadeautos.modules.vehicle.exception;

public class TarifaNoEncontradaException extends RuntimeException {
    public TarifaNoEncontradaException() {
        super("Tarifa no encontrada");
    }
}
