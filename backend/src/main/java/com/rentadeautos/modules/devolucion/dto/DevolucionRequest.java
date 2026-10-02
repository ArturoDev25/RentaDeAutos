package com.rentadeautos.modules.devolucion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos capturados al recibir el vehículo devuelto (S3-10).
 * Los límites de validación coinciden con chk_devoluciones_valores
 * y los tipos DECIMAL de la tabla devoluciones.
 */
public record DevolucionRequest(

        /** Identificador de la entrega a la que corresponde esta devolución. */
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser un identificador válido")
        Long entregaId,

        /**
         * Kilometraje del vehículo al momento de la devolución.
         * RN-08: no puede ser menor al kilometraje de salida de la entrega.
         */
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.0", message = "no puede ser negativo")
        @Digits(integer = 9, fraction = 1, message = "admite hasta 9 enteros y 1 decimal")
        BigDecimal kilometrajeEntrada,

        /** Nivel de combustible al recibir el vehículo (0–100). */
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.0", message = "debe estar entre 0 y 100")
        @DecimalMax(value = "100.0", message = "debe estar entre 0 y 100")
        @Digits(integer = 3, fraction = 2, message = "admite hasta 2 decimales")
        BigDecimal combustibleEntrada,

        /** Descripción del estado físico del vehículo al recibirlo. */
        @NotBlank(message = "es obligatoria")
        @Size(max = 2000, message = "admite hasta 2000 caracteres")
        String condicionEntrada,

        /**
         * Monto adicional por daños declarados.
         * Opcional; si es null se toma como 0.
         */
        @DecimalMin(value = "0.0", message = "no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "admite hasta 2 decimales")
        BigDecimal cargoDanos,

        /** Notas adicionales del agente (opcional). */
        @Size(max = 2000, message = "admite hasta 2000 caracteres")
        String observaciones,

        /**
         * Indica si el vehículo debe pasar a MANTENIMIENTO aunque no haya daños visibles.
         * Opcional; si es null se interpreta como false.
         */
        Boolean requiereMantenimiento
) {}
