package com.rentadeautos.modules.catalog.repository;

import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public class CatalogoRepository {
    private final JdbcTemplate jdbc;

    public CatalogoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<VehiculoCatalogoResponse> listar(LocalDateTime ahora) {
        return listar(ahora, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<VehiculoCatalogoResponse> listar(LocalDateTime ahora, Long categoriaId,
            BigDecimal precioMin, BigDecimal precioMax) {
        List<Object> parametros = new ArrayList<>(List.of(ahora.toLocalDate(),
                ahora.toLocalDate(), ahora.toLocalDate(), ahora.toLocalDate(), ahora, ahora));
        StringBuilder sql = new StringBuilder("""
                SELECT v.id, v.marca, v.modelo, v.anio, v.color,
                       c.id AS categoria_id, c.nombre AS categoria, t.precio_dia,
                       (v.imagen IS NOT NULL AND v.imagen <> '') AS tiene_imagen
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
                """);
        if (categoriaId != null) {
            sql.append(" AND c.id = ?");
            parametros.add(categoriaId);
        }
        if (precioMin != null) {
            sql.append(" AND t.precio_dia >= ?");
            parametros.add(precioMin);
        }
        if (precioMax != null) {
            sql.append(" AND t.precio_dia <= ?");
            parametros.add(precioMax);
        }
        sql.append(" ORDER BY v.marca, v.modelo, v.id");
        return jdbc.query(sql.toString(), (rs, fila) -> new VehiculoCatalogoResponse(rs.getLong("id"),
                        rs.getString("marca"), rs.getString("modelo"), rs.getInt("anio"),
                        rs.getString("color"), rs.getLong("categoria_id"),
                        rs.getString("categoria"), rs.getBigDecimal("precio_dia"),
                        rs.getBoolean("tiene_imagen")),
                parametros.toArray());
    }

    /** Foto del vehículo como data URL; vacío si no tiene o si está dado de baja. */
    @Transactional(readOnly = true)
    public Optional<String> imagen(Long vehiculoId) {
        return jdbc.query("""
                SELECT v.imagen FROM vehiculos v
                WHERE v.id = ? AND v.estado <> 'BAJA'
                  AND v.imagen IS NOT NULL AND v.imagen <> ''
                """, (rs, fila) -> rs.getString("imagen"), vehiculoId)
                .stream().findFirst();
    }
}
