package com.rentadeautos.modules.vehicle.dto;

import com.rentadeautos.modules.vehicle.model.Tarifa;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaResponse(Long id, Long categoriaId, String categoriaNombre,
        BigDecimal precioDia, BigDecimal cargoAtrasoDia, LocalDate fechaInicio,
        LocalDate fechaFin, Boolean activo) {
    public static TarifaResponse desde(Tarifa tarifa) {
        return new TarifaResponse(tarifa.getId(), tarifa.getCategoria().getId(),
                tarifa.getCategoria().getNombre(), tarifa.getPrecioDia(),
                tarifa.getCargoAtrasoDia(), tarifa.getFechaInicio(),
                tarifa.getFechaFin(), tarifa.getActivo());
    }
}
