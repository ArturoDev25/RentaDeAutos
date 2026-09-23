package com.rentadeautos.modules.vehicle.model;

/**
 * Estados operativos de un vehículo (S2-06).
 * Coinciden con la restricción chk_vehiculos_estado de V1__init_schema.sql.
 */
public enum EstadoVehiculo {
    DISPONIBLE,
    RESERVADO,
    RENTADO,
    MANTENIMIENTO,
    BAJA
}
