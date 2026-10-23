package com.rentadeautos.modules.reservation.dto;

import java.math.BigDecimal;

/** Desglose por estado de las operaciones del cliente en el periodo seleccionado. */
public record EstadoResumenItem(String estado, long cantidad, BigDecimal porcentaje) {
}
