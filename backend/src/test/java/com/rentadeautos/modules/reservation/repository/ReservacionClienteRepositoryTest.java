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
}
