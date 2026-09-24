package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.ConteoResponse;
import com.rentadeautos.modules.report.dto.ReservacionesResponse;
import com.rentadeautos.modules.report.repository.ReservacionReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Genera el reporte de reservaciones (S2-17).
 */
@Service
public class ReporteReservacionesService {

    private final ReservacionReporteRepository reservacionReporteRepository;

    public ReporteReservacionesService(ReservacionReporteRepository reservacionReporteRepository) {
        this.reservacionReporteRepository = reservacionReporteRepository;
    }

    @Transactional(readOnly = true)
    public ReservacionesResponse generar() {
        List<ConteoResponse> porEstado = reservacionReporteRepository.contarPorEstado().stream()
                .map(fila -> new ConteoResponse(fila.getEstado(), fila.getCantidad()))
                .toList();

        long total = porEstado.stream().mapToLong(ConteoResponse::cantidad).sum();

        return new ReservacionesResponse(total, porEstado);
    }
}
