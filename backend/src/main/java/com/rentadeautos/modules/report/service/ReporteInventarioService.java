package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.ConteoResponse;
import com.rentadeautos.modules.report.dto.InventarioResponse;
import com.rentadeautos.modules.report.repository.VehiculoReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Genera el reporte de inventario de vehículos (S2-17).
 */
@Service
public class ReporteInventarioService {

    private final VehiculoReporteRepository vehiculoReporteRepository;

    public ReporteInventarioService(VehiculoReporteRepository vehiculoReporteRepository) {
        this.vehiculoReporteRepository = vehiculoReporteRepository;
    }

    @Transactional(readOnly = true)
    public InventarioResponse generar() {
        List<ConteoResponse> porEstado = vehiculoReporteRepository.contarPorEstado().stream()
                .map(fila -> new ConteoResponse(fila.getEstado(), fila.getCantidad()))
                .toList();

        List<ConteoResponse> porCategoria = vehiculoReporteRepository.contarPorCategoria().stream()
                .map(fila -> new ConteoResponse(fila.getCategoria(), fila.getCantidad()))
                .toList();

        long total = porEstado.stream().mapToLong(ConteoResponse::cantidad).sum();

        return new InventarioResponse(total, porEstado, porCategoria);
    }
}
