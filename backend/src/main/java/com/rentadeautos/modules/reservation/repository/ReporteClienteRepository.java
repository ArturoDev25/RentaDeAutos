package com.rentadeautos.modules.reservation.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ReporteClienteRepository {
    private final JdbcTemplate jdbc;

    public record OperacionClienteFila(
            Long reservacionId,
            String marca,
            String modelo,
            Integer anio,
            String categoria,
            String estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            LocalDateTime fechaEntrega,
            LocalDateTime fechaDevolucionReal,
            BigDecimal tarifaDia,
            BigDecimal totalEstimado,
            BigDecimal totalFinal
    ) {
        public LocalDateTime fechaReferencia() {
            return fechaEntrega != null ? fechaEntrega : fechaInicio;
        }
    }

    public ReporteClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Obtiene exclusivamente las operaciones (reservaciones y rentas) del cliente autenticado
     * cuya fecha de referencia (entrega o inicio de reservación) sea mayor o igual a {@code desde}.
     */
    public List<OperacionClienteFila> listarOperacionesDesde(Long clienteId, LocalDateTime desde) {
        return jdbc.query("""
                SELECT r.id, v.marca, v.modelo, v.anio, c.nombre AS categoria,
                       r.estado, r.fecha_inicio, r.fecha_fin, e.fecha_entrega,
                       d.fecha_devolucion, r.tarifa_dia, r.total_estimado, d.total_final
                FROM reservaciones r
                LEFT JOIN vehiculos v ON v.id = r.vehiculo_id
                LEFT JOIN categorias c ON c.id = v.categoria_id
                LEFT JOIN entregas e ON e.reservacion_id = r.id
                LEFT JOIN devoluciones d ON d.entrega_id = e.id
                WHERE r.cliente_id = ?
                  AND COALESCE(e.fecha_entrega, r.fecha_inicio) >= ?
                ORDER BY COALESCE(e.fecha_entrega, r.fecha_inicio) DESC, r.id DESC
                """, (rs, fila) -> {
            var entrega = rs.getTimestamp("fecha_entrega");
            var devolucion = rs.getTimestamp("fecha_devolucion");
            int anioRaw = rs.getInt("anio");
            Integer anio = rs.wasNull() ? null : anioRaw;
            return new OperacionClienteFila(
                    rs.getLong("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    anio,
                    rs.getString("categoria"),
                    rs.getString("estado"),
                    rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                    rs.getTimestamp("fecha_fin").toLocalDateTime(),
                    entrega == null ? null : entrega.toLocalDateTime(),
                    devolucion == null ? null : devolucion.toLocalDateTime(),
                    rs.getBigDecimal("tarifa_dia"),
                    rs.getBigDecimal("total_estimado"),
                    rs.getBigDecimal("total_final")
            );
        }, clienteId, desde);
    }
}
