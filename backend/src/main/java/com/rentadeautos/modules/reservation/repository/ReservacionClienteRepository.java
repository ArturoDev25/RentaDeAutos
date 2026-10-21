package com.rentadeautos.modules.reservation.repository;

import com.rentadeautos.modules.reservation.dto.ReservacionClienteResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ReservacionClienteRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT id, vehiculo_id, fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado
            FROM reservaciones WHERE cliente_id = ?
            """;
    private static final RowMapper<ReservacionClienteResponse> MAPEO = (rs, fila) ->
            new ReservacionClienteResponse(rs.getLong("id"), rs.getLong("vehiculo_id"),
                    rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                    rs.getTimestamp("fecha_fin").toLocalDateTime(), rs.getString("estado"),
                    rs.getBigDecimal("tarifa_dia"), rs.getBigDecimal("total_estimado"));

    public ReservacionClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReservacionClienteResponse> listar(Long clienteId) {
        return jdbc.query(SELECT + " ORDER BY fecha_inicio DESC, id DESC", MAPEO, clienteId);
    }

    public Optional<ReservacionClienteResponse> obtener(Long clienteId, Long id) {
        return jdbc.query(SELECT + " AND id = ?", MAPEO, clienteId, id).stream().findFirst();
    }
}
