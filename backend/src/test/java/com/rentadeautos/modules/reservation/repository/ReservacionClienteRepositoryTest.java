package com.rentadeautos.modules.reservation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import(ReservacionClienteRepository.class)
class ReservacionClienteRepositoryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservacionClienteRepository reservaciones;

    private void crear(long id, long cliente) {
        jdbc.update("""
                INSERT INTO reservaciones (id, cliente_id, vehiculo_id, creado_por_id,
                fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (?, ?, 1, 1, '2026-11-01 10:00:00', '2026-11-02 10:00:00', 'PENDIENTE', 850, 850)
                """, id, cliente);
    }

    @Test void listaSoloRegistrosDelClienteYOrdenaEmpatesPorIdDescendente() {
        crear(1, 7); crear(2, 8); crear(3, 7);
        assertEquals(java.util.List.of(3L, 1L), reservaciones.listar(7L).stream().map(r -> r.id()).toList());
    }

    @Test void noSePuedeObtenerReservaAjenaPorId() {
        crear(1, 8);
        assertTrue(reservaciones.obtener(7L, 1L).isEmpty());
        assertEquals(1L, reservaciones.obtener(8L, 1L).orElseThrow().id());
    }

    @Test void clienteSinRegistrosRecibeListaVacia() {
        assertTrue(reservaciones.listar(7L).isEmpty());
    }

    @Test void identificadorInexistenteNoDevuelveDatos() {
        crear(1, 7);
        assertTrue(reservaciones.obtener(7L, 99L).isEmpty());
    }
    private void vehiculoHistorial() {
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (1, 'SUV', 0, TRUE)");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (1, 1, 'DEMO1', '1HGCM82633A004352', 'Nissan', 'Kicks', 2024, 1000, 'DISPONIBLE')
                """);
    }

    private void renta(long id, long cliente, String estado) {
        crear(id, cliente);
        jdbc.update("UPDATE reservaciones SET estado = ? WHERE id = ?", estado, id);
        jdbc.update("""
                INSERT INTO entregas (id, reservacion_id, entregado_por_id, fecha_entrega,
                    kilometraje_salida, combustible_salida, condicion_salida)
                VALUES (?, ?, 1, '2026-11-01 10:00:00', 1000, 100, 'Sin daños')
                """, id, id);
    }

    @Test void historialSinRentasDevuelveListaVacia() {
        assertTrue(reservaciones.historial(7L).isEmpty());
    }

    @Test void historialSoloMuestraRentasPropiasSinInventarTotalFinal() {
        vehiculoHistorial();
        renta(1, 7, "EN_CURSO"); renta(2, 8, "EN_CURSO");
        var lista = reservaciones.historial(7L);
        assertEquals(1, lista.size());
        var r = lista.get(0);
        assertEquals(1L, r.reservacionId());
        assertEquals("Nissan", r.marca());
        assertEquals("Kicks", r.modelo());
        assertNull(r.fechaDevolucionReal());
        assertNull(r.totalFinal());
    }

    @Test void historialFinalizadoIncluyeDevolucionYTotalGuardado() {
        vehiculoHistorial(); renta(1, 7, "FINALIZADA");
        jdbc.update("""
                INSERT INTO devoluciones (id, entrega_id, recibido_por_id, fecha_devolucion,
                    kilometraje_entrada, combustible_entrada, condicion_entrada, cargo_atraso,
                    cargo_danos, total_final)
                VALUES (1, 1, 1, '2026-11-02 11:00:00', 1100, 100, 'Sin daños', 100, 0, 950)
                """);
        var r = reservaciones.historial(7L).get(0);
        assertEquals(java.time.LocalDateTime.of(2026, 11, 2, 11, 0), r.fechaDevolucionReal());
        assertEquals(new java.math.BigDecimal("950.00"), r.totalFinal());
        assertEquals(new java.math.BigDecimal("850.00"), r.tarifaDia());
        assertEquals(new java.math.BigDecimal("850.00"), r.totalEstimado());
    }

    @Test void historialExcluyeReservasQueNoSonRentas() {
        vehiculoHistorial();
        renta(1, 7, "PENDIENTE"); renta(2, 7, "CONFIRMADA"); renta(3, 7, "CANCELADA");
        assertTrue(reservaciones.historial(7L).isEmpty());
    }

    @Test void historialExigeEntregaRegistrada() {
        vehiculoHistorial(); crear(1, 7);
        jdbc.update("UPDATE reservaciones SET estado = 'EN_CURSO' WHERE id = 1");
        assertTrue(reservaciones.historial(7L).isEmpty());
    }

    @Test void historialOrdenaPorEntregaDescendenteYDesempataPorId() {
        vehiculoHistorial();
        renta(1, 7, "EN_CURSO"); renta(2, 7, "FINALIZADA"); renta(3, 7, "FINALIZADA");
        jdbc.update("UPDATE entregas SET fecha_entrega = '2026-11-03 10:00:00' WHERE id = 1");
        assertEquals(java.util.List.of(1L, 3L, 2L), reservaciones.historial(7L).stream()
                .map(r -> r.reservacionId()).toList());
    }

}
