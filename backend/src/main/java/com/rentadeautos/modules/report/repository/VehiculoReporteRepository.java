package com.rentadeautos.modules.report.repository;

import com.rentadeautos.modules.report.model.VehiculoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Consultas de solo lectura sobre vehiculos para el módulo de reportes.
 */
public interface VehiculoReporteRepository extends JpaRepository<VehiculoReporte, Long> {

    @Query("""
            SELECT v.estado AS estado, COUNT(v) AS cantidad
            FROM VehiculoReporte v
            GROUP BY v.estado
            ORDER BY v.estado
            """)
    List<ConteoEstado> contarPorEstado();

    @Query("""
            SELECT c.nombre AS categoria, COUNT(v) AS cantidad
            FROM VehiculoReporte v
            JOIN v.categoria c
            GROUP BY c.nombre
            ORDER BY c.nombre
            """)
    List<ConteoCategoria> contarPorCategoria();

    interface ConteoEstado {
        String getEstado();
        Long getCantidad();
    }

    interface ConteoCategoria {
        String getCategoria();
        Long getCantidad();
    }
}
