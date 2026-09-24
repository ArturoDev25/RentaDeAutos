package com.rentadeautos.modules.report.repository;

import com.rentadeautos.modules.report.model.ClienteReporte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteReporteRepository extends JpaRepository<ClienteReporte, Long> {

    long countByActivo(Boolean activo);
}
