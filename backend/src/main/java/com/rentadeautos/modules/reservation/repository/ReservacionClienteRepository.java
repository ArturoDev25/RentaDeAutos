package com.rentadeautos.modules.reservation.repository;

import com.rentadeautos.modules.reservation.dto.ReservacionClienteResponse;
import com.rentadeautos.modules.reservation.dto.RentaClienteResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ReservacionClienteRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT r.id, r.vehiculo_id, r.fecha_inicio, r.fecha_fin, r.estado,
                   r.tarifa_dia, r.total_estimado, v.marca, v.modelo, v.anio, c.nombre AS categoria
            FROM reservaciones r
            LEFT JOIN vehiculos v ON v.id = r.vehiculo_id
            LEFT JOIN categorias c ON c.id = v.categoria_id
            WHERE r.cliente_id = ?
            """;
    private static final RowMapper<ReservacionClienteResponse> MAPEO = (rs, fila) -> {
        LocalDateTime fechaInicio = rs.getTimestamp("fecha_inicio").toLocalDateTime();
        LocalDateTime fechaFin = rs.getTimestamp("fecha_fin").toLocalDateTime();
        String estado = rs.getString("estado");
        int anioRaw = rs.getInt("anio");
        Integer anio = rs.wasNull() ? null : anioRaw;
        boolean puedeCancelar = ("PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado))
                && fechaInicio.isAfter(LocalDateTime.now());
        return new ReservacionClienteResponse(rs.getLong("id"), rs.getLong("vehiculo_id"),
                fechaInicio, fechaFin, estado,
                rs.getBigDecimal("tarifa_dia"), rs.getBigDecimal("total_estimado"),
                rs.getString("marca"), rs.getString("modelo"), anio, rs.getString("categoria"),
                puedeCancelar);
    };

    public ReservacionClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReservacionClienteResponse> listar(Long clienteId) {
        return jdbc.query(SELECT + " ORDER BY r.fecha_inicio DESC, r.id DESC", MAPEO, clienteId);
    }

    public Optional<ReservacionClienteResponse> obtener(Long clienteId, Long id) {
        return jdbc.query(SELECT + " AND r.id = ?", MAPEO, clienteId, id).stream().findFirst();
    }

    /** Solo rentas efectivamente iniciadas del dueño autenticado, no reservas pendientes. */
    public List<RentaClienteResponse> historial(Long clienteId) {
        return jdbc.query("""
                SELECT r.id, r.vehiculo_id, v.marca, v.modelo, v.anio, c.nombre AS categoria,
                       r.estado, e.fecha_entrega, r.fecha_fin, d.fecha_devolucion,
                       r.tarifa_dia, r.total_estimado, d.total_final
                FROM reservaciones r
                JOIN entregas e ON e.reservacion_id = r.id
                JOIN vehiculos v ON v.id = r.vehiculo_id
                LEFT JOIN categorias c ON c.id = v.categoria_id
                LEFT JOIN devoluciones d ON d.entrega_id = e.id
                WHERE r.cliente_id = ? AND r.estado IN ('EN_CURSO', 'FINALIZADA')
                ORDER BY e.fecha_entrega DESC, r.id DESC
                """, (rs, fila) -> {
                    var devolucion = rs.getTimestamp("fecha_devolucion");
                    int anioRaw = rs.getInt("anio");
                    Integer anio = rs.wasNull() ? null : anioRaw;
                    return new RentaClienteResponse(rs.getLong("id"), rs.getLong("vehiculo_id"),
                            rs.getString("marca"), rs.getString("modelo"), rs.getString("estado"),
                            rs.getTimestamp("fecha_entrega").toLocalDateTime(),
                            rs.getTimestamp("fecha_fin").toLocalDateTime(),
                            devolucion == null ? null : devolucion.toLocalDateTime(),
                            rs.getBigDecimal("tarifa_dia"), rs.getBigDecimal("total_estimado"),
                            rs.getBigDecimal("total_final"), anio, rs.getString("categoria"));
                }, clienteId);
    }

}
