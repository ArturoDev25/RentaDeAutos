package com.rentadeautos.modules.report.service;

import com.rentadeautos.modules.report.dto.ClientesResponse;
import com.rentadeautos.modules.report.repository.ClienteReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera el reporte de clientes (S2-17).
 */
@Service
public class ReporteClientesService {

    private final ClienteReporteRepository clienteReporteRepository;

    public ReporteClientesService(ClienteReporteRepository clienteReporteRepository) {
        this.clienteReporteRepository = clienteReporteRepository;
    }

    @Transactional(readOnly = true)
    public ClientesResponse generar() {
        long activos = clienteReporteRepository.countByActivo(true);
        long inactivos = clienteReporteRepository.countByActivo(false);

        return new ClientesResponse(activos + inactivos, activos, inactivos);
    }
}
