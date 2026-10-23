package com.rentadeautos.modules.reservation.repository;

import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.service.ReporteClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import({ReporteClienteRepository.class, ReporteClienteService.class})
class ReporteClienteRepositoryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ReporteClienteService servicio;

    @BeforeEach
    void prepararCatalogoBase() {
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (1, 'SUV', 0, TRUE)");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (1, 1, 'DEMO1', '1HGCM82633A004352', 'Nissan', 'Kicks', 2024, 1000, 'DISPONIBLE')
                """);
    }

    private void insertarOperacion(long id, long clienteId, String estado,
            LocalDateTime inicio, LocalDateTime fin, BigDecimal estimado, BigDecimal totalFinal) {
        jdbc.update("""
                INSERT INTO reservaciones (id, cliente_id, vehiculo_id, creado_por_id,
                    fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (?, ?, 1, 1, ?, ?, ?, 850, ?)
                """, id, clienteId, inicio, fin, estado, estimado);
        if ("EN_CURSO".equals(estado) || "FINALIZADA".equals(estado)) {
            jdbc.update("""
                    INSERT INTO entregas (id, reservacion_id, entregado_por_id, fecha_entrega,
                        kilometraje_salida, combustible_salida, condicion_salida)
                    VALUES (?, ?, 1, ?, 1000, 100, 'Sin daños')
                    """, id, id, inicio);
        }
        if ("FINALIZADA".equals(estado) && totalFinal != null) {
            jdbc.update("""
                    INSERT INTO devoluciones (id, entrega_id, recibido_por_id, fecha_devolucion,
                        kilometraje_entrada, combustible_entrada, condicion_entrada, cargo_atraso,
                        cargo_danos, total_final)
                    VALUES (?, ?, 1, ?, 1200, 100, 'Sin daños', 0, 0, ?)
                    """, id, id, fin, totalFinal);
        }
    }

    @Test
    void clienteSinOperacionesRecibeCerosYAvisoExplicitamente() {
        var r = servicio.generarResumen(7L, "3M");
        assertEquals("3M", r.periodo());
        assertEquals(0L, r.totalRentas());
        assertEquals(new BigDecimal("0.00"), r.gastoTotalEstimado());
        assertEquals(0L, r.diasRentados());
        assertEquals(new BigDecimal("0.00"), r.costoPromedioDia());
        assertTrue(r.historial().isEmpty());
        assertEquals(ReporteClienteService.AVISO_IMPORTES, r.avisoImportes());
    }

    @Test
    void aislaOperacionesPorClienteYCalculaKpisEImportesEstimados() {
        LocalDateTime hace5Dias = LocalDateTime.now().minusDays(5).withNano(0);
        // Renta finalizada de cliente 7 (2 días, total final 1900)
        insertarOperacion(1L, 7L, "FINALIZADA", hace5Dias, hace5Dias.plusDays(2),
                new BigDecimal("1700.00"), new BigDecimal("1900.00"));
        // Renta activa de cliente 7 (3 días previstos, sin devolución -> usa totalEstimado 2550)
        insertarOperacion(2L, 7L, "EN_CURSO", hace5Dias.plusDays(1), hace5Dias.plusDays(4),
                new BigDecimal("2550.00"), null);
        // Reservación pendiente de cliente 7 (no cuenta como renta en KPI pero aparece en distribución/historial)
        insertarOperacion(3L, 7L, "PENDIENTE", hace5Dias.plusDays(2), hace5Dias.plusDays(3),
                new BigDecimal("850.00"), null);
        // Renta de otro cliente (cliente 8) -> jamás debe mezclarse
        insertarOperacion(4L, 8L, "FINALIZADA", hace5Dias, hace5Dias.plusDays(5),
                new BigDecimal("4250.00"), new BigDecimal("5000.00"));

        var r = servicio.generarResumen(7L, "30D");
        assertEquals(2L, r.totalRentas());
        assertEquals(new BigDecimal("4450.00"), r.gastoTotalEstimado());
        assertEquals(5L, r.diasRentados());
        assertEquals(new BigDecimal("890.00"), r.costoPromedioDia());
        assertEquals(3, r.historial().size());
        assertTrue(r.historial().stream().noneMatch(h -> h.reservacionId().equals(4L)));
    }

    @Test
    void periodoInvalidoLanza400() {
        var ex = assertThrows(ReservacionException.class,
                () -> servicio.generarResumen(7L, "INVALIDO"));
        assertEquals(400, ex.getStatus().value());
    }
}
