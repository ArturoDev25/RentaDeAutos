package com.rentadeautos.modules.rental.repository;

import com.rentadeautos.modules.rental.dto.RentaActivaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class RentaActivaRepository {
    private final JdbcTemplate jdbc;

    public RentaActivaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Incluye las vencidas mientras sigan EN_CURSO; no filtra por fecha actual. */
    public List<RentaActivaResponse> listar() {
        return jdbc.query("""
                SELECT r.id AS reservacion_id, c.id AS cliente_id, c.nombre, c.apellidos,
                       c.telefono, v.id AS vehiculo_id, v.marca, v.modelo, v.placa,
                       r.fecha_inicio, r.fecha_fin, r.estado, r.tarifa_dia, r.total_estimado
                FROM reservaciones r
                JOIN clientes c ON c.id = r.cliente_id
                JOIN vehiculos v ON v.id = r.vehiculo_id
                WHERE r.estado = 'EN_CURSO'
                ORDER BY r.fecha_fin ASC, r.id ASC
                """, (rs, fila) -> new RentaActivaResponse(
                rs.getLong("reservacion_id"), rs.getLong("cliente_id"),
                rs.getString("nombre") + " " + rs.getString("apellidos"),
                rs.getString("telefono"), rs.getLong("vehiculo_id"),
                rs.getString("marca"), rs.getString("modelo"), rs.getString("placa"),
                rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                rs.getTimestamp("fecha_fin").toLocalDateTime(), rs.getString("estado"),
                rs.getBigDecimal("tarifa_dia"), rs.getBigDecimal("total_estimado")));
    }
}
