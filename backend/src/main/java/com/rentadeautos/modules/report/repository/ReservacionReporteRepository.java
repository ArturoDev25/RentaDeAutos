package com.rentadeautos.modules.report.repository;

import com.rentadeautos.modules.report.model.ReservacionReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Consultas de solo lectura sobre reservaciones para el módulo de reportes.
 */
public interface ReservacionReporteRepository extends JpaRepository<ReservacionReporte, Long> {

    @Query("""
            SELECT r.estado AS estado, COUNT(r) AS cantidad
            FROM ReservacionReporte r
            GROUP BY r.estado
            ORDER BY r.estado
            """)
    List<ConteoEstadoReservacion> contarPorEstado();

    interface ConteoEstadoReservacion {
        String getEstado();
        Long getCantidad();
    }
}
