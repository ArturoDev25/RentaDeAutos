package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Escenarios sintéticos persistentes: no ejecuta ni suplanta acciones de usuarios reales. */
@Component
@Order(50)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class ReservacionesDemoInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final TarifaService tarifas;
    private record Auto(Long id, Long categoriaId, String estado, BigDecimal kilometraje) { }
    private record Caso(int auto, String estado, int inicio, int dias) { }
    private static final List<Caso> CASOS = List.of(
            new Caso(1, "PENDIENTE", 5, 3), new Caso(2, "CONFIRMADA", 2, 3),
            new Caso(3, "EN_CURSO", -1, 3), new Caso(4, "FINALIZADA", -6, 3),
            new Caso(5, "CANCELADA", 9, 3));

    public ReservacionesDemoInitializer(JdbcTemplate jdbc, TarifaService tarifas) {
        this.jdbc = jdbc;
        this.tarifas = tarifas;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var clientes = jdbc.query("SELECT id FROM clientes WHERE correo = ? AND numero_licencia = ? AND activo = TRUE",
                (rs, fila) -> rs.getLong(1), "demo01@s504.example", "S504-DEMO-01");
        if (clientes.size() != 1) {
            throw new IllegalStateException("Cargar primero el cliente demo01 activo de S5-04");
        }
        Long cliente = clientes.get(0);
        var operadores = jdbc.query("""
                SELECT u.id FROM usuarios_app u JOIN roles r ON r.id = u.rol_id
                WHERE u.correo = 'admin@demo.local' AND u.activo = TRUE
                  AND r.activo = TRUE AND r.nombre = 'ADMINISTRADOR'
                """, (rs, fila) -> rs.getLong(1));
        if (operadores.size() != 1) {
            throw new IllegalStateException("Falta el administrador ficticio activo admin@demo.local");
        }
        Long operador = operadores.get(0);
        LocalDate hoy = LocalDate.now();
        for (Caso caso : CASOS) {
            String placa = String.format(Locale.ROOT, "S504-DEMO-%02d", caso.auto());
            String vin = String.format(Locale.ROOT, "S504DEMA%09d", caso.auto());
            var autos = jdbc.query("""
                    SELECT id, categoria_id, estado, kilometraje FROM vehiculos
                    WHERE placa = ? AND vin = ? FOR UPDATE
                    """, (rs, fila) -> new Auto(rs.getLong("id"), rs.getLong("categoria_id"),
                            rs.getString("estado"), rs.getBigDecimal("kilometraje")), placa, vin);
            if (autos.size() != 1) {
                throw new IllegalStateException("Falta vehículo del catálogo demo: " + placa);
            }
            Auto auto = autos.get(0);
            String marca = "[S504-DEMO-" + caso.estado() + "]";
            var existentes = jdbc.query("SELECT cliente_id, vehiculo_id FROM reservaciones WHERE observaciones = ? FOR UPDATE",
                    (rs, fila) -> List.of(rs.getLong(1), rs.getLong(2)), marca);
            if (!existentes.isEmpty()) {
                if (existentes.size() != 1 || !existentes.get(0).equals(List.of(cliente, auto.id()))) {
                    throw new IllegalStateException("Colisión de escenario demo: " + marca);
                }
                continue; // Conserva fecha, importe y cambios realizados durante la presentación.
            }
            if (!"DISPONIBLE".equals(auto.estado())) {
                throw new IllegalStateException("No se cambia el estado del vehículo existente: " + placa);
            }
            LocalDateTime inicio = hoy.plusDays(caso.inicio()).atTime(9, 0);
            LocalDateTime fin = inicio.plusDays(caso.dias());
            var conflictos = jdbc.query("""
                    SELECT id FROM reservaciones WHERE vehiculo_id = ?
                      AND (estado = 'EN_CURSO' OR
                           (estado IN ('PENDIENTE', 'CONFIRMADA') AND fecha_inicio < ? AND fecha_fin > ?))
                    FOR UPDATE
                    """, (rs, fila) -> rs.getLong(1), auto.id(), fin, inicio);
            if (!conflictos.isEmpty()) {
                throw new IllegalStateException("Existe un compromiso previo en " + placa);
            }
            BigDecimal precio = precioHistorico(auto.categoriaId(), inicio.toLocalDate(), hoy);
            BigDecimal total = precio.multiply(BigDecimal.valueOf(caso.dias()));
            long reserva = insertar("""
                    INSERT INTO reservaciones (cliente_id, vehiculo_id, creado_por_cliente_id, fecha_inicio,
                        fecha_fin, estado, tarifa_dia, total_estimado, observaciones)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, cliente, auto.id(), cliente, inicio, fin, caso.estado(), precio, total, marca);
            if ("CONFIRMADA".equals(caso.estado())) {
                jdbc.update("UPDATE vehiculos SET estado = 'RESERVADO' WHERE id = ?", auto.id());
            }
            if (List.of("EN_CURSO", "FINALIZADA").contains(caso.estado())) {
                BigDecimal salida = auto.kilometraje();
                if ("FINALIZADA".equals(caso.estado())) {
                    salida = salida.subtract(new BigDecimal("100.0")).max(BigDecimal.ZERO);
                }
                long entrega = insertar("""
                        INSERT INTO entregas (reservacion_id, entregado_por_id, fecha_entrega,
                            kilometraje_salida, combustible_salida, condicion_salida, observaciones)
                        VALUES (?, ?, ?, ?, 100, 'Sin daños de demostración', ?)
                        """, reserva, operador, inicio, salida, marca);
                if ("EN_CURSO".equals(caso.estado())) {
                    jdbc.update("UPDATE vehiculos SET estado = 'RENTADO' WHERE id = ?", auto.id());
                } else {
                    insertar("""
                            INSERT INTO devoluciones (entrega_id, recibido_por_id, fecha_devolucion,
                                kilometraje_entrada, combustible_entrada, condicion_entrada,
                                cargo_atraso, cargo_danos, total_final, observaciones)
                            VALUES (?, ?, ?, ?, 100, 'Sin daños de demostración', 0, 0, ?, ?)
                            """, entrega, operador, fin, auto.kilometraje(), total, marca);
                }
            }
        }
    }

    private BigDecimal precioHistorico(Long categoria, LocalDate inicio, LocalDate hoy) {
        var precios = jdbc.query("""
                SELECT precio_dia FROM tarifas WHERE categoria_id = ? AND activo = TRUE
                AND fecha_inicio <= ? AND (fecha_fin IS NULL OR fecha_fin >= ?) FOR UPDATE
                """, (rs, fila) -> rs.getBigDecimal(1), categoria, inicio, inicio);
        if (precios.size() == 1) return precios.get(0);
        if (!precios.isEmpty()) throw new IllegalStateException("Tarifas demo traslapadas");
        // Completa únicamente un hueco histórico; nunca edita una tarifa existente.
        var futuras = jdbc.query("""
                SELECT fecha_inicio FROM tarifas WHERE categoria_id = ? AND activo = TRUE
                AND fecha_inicio > ? ORDER BY fecha_inicio FOR UPDATE
                """, (rs, fila) -> rs.getDate(1).toLocalDate(), categoria, inicio);
        var actuales = jdbc.query("""
                SELECT precio_dia FROM tarifas WHERE categoria_id = ? AND activo = TRUE
                AND fecha_inicio <= ? AND (fecha_fin IS NULL OR fecha_fin >= ?) FOR UPDATE
                """, (rs, fila) -> rs.getBigDecimal(1), categoria, hoy, hoy);
        if (actuales.size() != 1 || futuras.isEmpty()) {
            throw new IllegalStateException("No hay una tarifa demo válida para completar el escenario");
        }
        tarifas.crear(new TarifaRequest(categoria, actuales.get(0), new BigDecimal("100.00"),
                inicio, futuras.get(0).minusDays(1)));
        return actuales.get(0);
    }

    private long insertar(String sql, Object... parametros) {
        var clave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement(sql, new String[] {"id"});
            for (int i = 0; i < parametros.length; i++) sentencia.setObject(i + 1, parametros[i]);
            return sentencia;
        }, clave);
        Number id = clave.getKey();
        if (id == null) throw new IllegalStateException("No se obtuvo el ID del registro demo");
        return id.longValue();
    }
}
