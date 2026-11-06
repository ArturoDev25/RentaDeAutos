package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import com.rentadeautos.modules.rental.repository.RentaActivaRepository;
import com.rentadeautos.modules.reservation.repository.ReservacionClienteRepository;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_reservas;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=never"
})
@Import(TarifaService.class)
class ReservacionesDemoInitializerTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaRepository categorias;
    @Autowired VehiculoRepository vehiculos;
    @Autowired TarifaRepository tarifas;
    @Autowired TarifaService servicio;
    private ReservacionesDemoInitializer seed;

    @BeforeEach void preparar() {
        new ClienteDemoInitializer(clientes, new BCryptPasswordEncoder(4), "Demo-S504-test-2026").run();
        new CatalogoDemoInitializer(categorias, vehiculos, tarifas, servicio).run();
        seed = new ReservacionesDemoInitializer(jdbc, servicio);
    }

    private int cantidad(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    @Test void creaCincoEstadosConDosEntregasYUnaDevolucion() {
        seed.run();
        assertEquals(5, cantidad("reservaciones"));
        assertEquals(2, cantidad("entregas"));
        assertEquals(1, cantidad("devoluciones"));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(DISTINCT estado) FROM reservaciones", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones WHERE creado_por_id IS NOT NULL OR creado_por_cliente_id <> cliente_id", Integer.class));
    }

    @Test void portalYPanelRecibenDatosSinCambiarSusConsultas() {
        seed.run();
        Long cliente = clientes.findByCorreoIgnoreCase("demo01@s504.example").orElseThrow().getId();
        var consultas = new ReservacionClienteRepository(jdbc);
        assertEquals(5, consultas.listar(cliente).size());
        assertEquals(2, consultas.historial(cliente).size());
        Long otro = clientes.findByCorreoIgnoreCase("demo02@s504.example").orElseThrow().getId();
        assertTrue(consultas.listar(otro).isEmpty());
        assertEquals(1, new RentaActivaRepository(jdbc).listar().size());
        assertEquals(3, new CatalogoRepository(jdbc).listar(LocalDateTime.now()).size());
    }

    @Test void entregaYDevolucionMantienenFechasKilometrajeYTotalCoherentes() {
        seed.run();
        jdbc.query("""
                SELECT r.fecha_inicio, r.fecha_fin, r.tarifa_dia, r.total_estimado,
                       e.fecha_entrega, e.kilometraje_salida, d.fecha_devolucion,
                       d.kilometraje_entrada, d.total_final, d.cargo_atraso, d.cargo_danos
                FROM reservaciones r JOIN entregas e ON e.reservacion_id = r.id
                JOIN devoluciones d ON d.entrega_id = e.id
                """, rs -> {
            assertEquals(rs.getTimestamp("fecha_inicio"), rs.getTimestamp("fecha_entrega"));
            assertEquals(rs.getTimestamp("fecha_fin"), rs.getTimestamp("fecha_devolucion"));
            assertTrue(rs.getBigDecimal("kilometraje_entrada").compareTo(rs.getBigDecimal("kilometraje_salida")) >= 0);
            assertEquals(0, rs.getBigDecimal("tarifa_dia").multiply(new BigDecimal("3"))
                    .compareTo(rs.getBigDecimal("total_estimado")));
            assertEquals(0, rs.getBigDecimal("total_estimado").compareTo(rs.getBigDecimal("total_final")));
            assertEquals(0, rs.getBigDecimal("cargo_atraso").signum());
            assertEquals(0, rs.getBigDecimal("cargo_danos").signum());
        });
        assertEquals(1, cantidad("devoluciones"));
    }

    @Test void repetirNoDuplicaNiRevierteCambiosDeLaPresentacion() {
        seed.run();
        jdbc.update("UPDATE reservaciones SET estado = 'CANCELADA' WHERE observaciones = '[S504-DEMO-PENDIENTE]'");
        var fechas = jdbc.queryForList("SELECT fecha_inicio FROM reservaciones ORDER BY id");
        int tarifasAntes = cantidad("tarifas");
        seed.run();
        assertEquals(5, cantidad("reservaciones"));
        assertEquals(2, cantidad("entregas"));
        assertEquals(1, cantidad("devoluciones"));
        assertEquals(tarifasAntes, cantidad("tarifas"));
        assertEquals(fechas, jdbc.queryForList("SELECT fecha_inicio FROM reservaciones ORDER BY id"));
        assertEquals("CANCELADA", jdbc.queryForObject("SELECT estado FROM reservaciones WHERE observaciones = '[S504-DEMO-PENDIENTE]'", String.class));
    }

    @Test void cadaEscenarioUsaUnaTarifaVigenteEnSuInicioSinTraslapes() {
        seed.run();
        var conteos = jdbc.queryForList("""
                SELECT r.id, COUNT(t.id) AS numero FROM reservaciones r
                JOIN vehiculos v ON v.id = r.vehiculo_id
                JOIN tarifas t ON t.categoria_id = v.categoria_id AND t.activo = TRUE
                  AND t.fecha_inicio <= CAST(r.fecha_inicio AS DATE)
                  AND (t.fecha_fin IS NULL OR t.fecha_fin >= CAST(r.fecha_inicio AS DATE))
                GROUP BY r.id
                """);
        assertEquals(5, conteos.size());
        assertTrue(conteos.stream().allMatch(f -> ((Number) f.get("NUMERO")).intValue() == 1));
    }

    @Test void noModificaVehiculoNoDisponibleParaCrearEscenario() {
        jdbc.update("UPDATE vehiculos SET estado = 'MANTENIMIENTO' WHERE placa = 'S504-DEMO-01'");
        assertThrows(IllegalStateException.class, () -> seed.run());
        assertEquals(0, cantidad("reservaciones"));
        assertEquals("MANTENIMIENTO", jdbc.queryForObject("SELECT estado FROM vehiculos WHERE placa = 'S504-DEMO-01'", String.class));
    }

    @Test void requiereClienteDemoActivo() {
        jdbc.update("UPDATE clientes SET activo = FALSE WHERE correo = 'demo01@s504.example'");
        assertThrows(IllegalStateException.class, () -> seed.run());
        assertEquals(0, cantidad("reservaciones"));
    }
}
