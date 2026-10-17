package com.rentadeautos.modules.catalog.repository;

import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CatalogoRepository {
    private final JdbcTemplate jdbc;

    public CatalogoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<VehiculoCatalogoResponse> listar(LocalDateTime ahora) {
        return jdbc.query("""
                SELECT v.id, v.marca, v.modelo, v.anio, v.color,
                       c.id AS categoria_id, c.nombre AS categoria, t.precio_dia
                FROM vehiculos v
                JOIN categorias c ON c.id = v.categoria_id
                JOIN tarifas t ON t.categoria_id = c.id
                WHERE v.estado = 'DISPONIBLE' AND c.activo = TRUE
                  AND t.activo = TRUE AND t.precio_dia > 0
                  AND t.fecha_inicio <= ? AND (t.fecha_fin IS NULL OR t.fecha_fin >= ?)
                  AND (SELECT COUNT(*) FROM tarifas otra
                       WHERE otra.categoria_id = c.id AND otra.activo = TRUE
                         AND otra.fecha_inicio <= ?
                         AND (otra.fecha_fin IS NULL OR otra.fecha_fin >= ?)) = 1
                  AND NOT EXISTS (SELECT 1 FROM reservaciones r WHERE r.vehiculo_id = v.id
                       AND (r.estado = 'EN_CURSO' OR
                           (r.estado IN ('PENDIENTE', 'CONFIRMADA')
                            AND r.fecha_inicio <= ? AND r.fecha_fin > ?)))
                ORDER BY v.marca, v.modelo, v.id
                """, (rs, fila) -> new VehiculoCatalogoResponse(rs.getLong("id"),
                        rs.getString("marca"), rs.getString("modelo"), rs.getInt("anio"),
                        rs.getString("color"), rs.getLong("categoria_id"),
                        rs.getString("categoria"), rs.getBigDecimal("precio_dia")),
                ahora.toLocalDate(), ahora.toLocalDate(), ahora.toLocalDate(),
                ahora.toLocalDate(), ahora, ahora);
    }
}
