package com.rentadeautos.modules.rental.repository;

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
@Import(RentaActivaRepository.class)
class RentaActivaRepositoryTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RentaActivaRepository rentas;
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 10, 0);

    @BeforeEach void datosOperativos() {
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (1, 'SUV', 0, TRUE)");
        jdbc.update("""
                INSERT INTO clientes (id, nombre, apellidos, telefono, numero_licencia,
                    licencia_vencimiento, activo)
                VALUES (1, 'Ana', 'Pérez', '8441234567', 'DEMO-001', '2027-12-31', TRUE)
                """);
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (1, 1, 'DEM-1234', '1HGCM82633A004352', 'Nissan', 'Kicks', 2024, 1000, 'RENTADO')
                """);
    }

    private void reserva(long id, String estado, LocalDateTime fin) {
        jdbc.update("""
                INSERT INTO reservaciones (id, cliente_id, vehiculo_id, creado_por_id,
                    fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (?, 1, 1, 1, ?, ?, ?, 850.00, 2550.00)
                """, id, inicio, fin, estado);
    }

    @Test void sinRentasDevuelveListaVacia() {
        assertTrue(rentas.listar().isEmpty());
    }

    @Test void soloIncluyeEnCurso() {
        reserva(1, "PENDIENTE", inicio.plusDays(3));
        reserva(2, "CONFIRMADA", inicio.plusDays(3));
        reserva(3, "CANCELADA", inicio.plusDays(3));
        reserva(4, "FINALIZADA", inicio.plusDays(3));
        reserva(5, "EN_CURSO", inicio.plusDays(3));
        assertEquals(java.util.List.of(5L), rentas.listar().stream()
                .map(r -> r.reservacionId()).toList());
    }

    @Test void devuelveClienteVehiculoFechaYTarifaHistorica() {
        reserva(1, "EN_CURSO", inicio.plusDays(3));
        var resultado = rentas.listar().get(0);
        assertEquals("Ana Pérez", resultado.clienteNombre());
        assertEquals("8441234567", resultado.clienteTelefono());
        assertEquals("Nissan", resultado.marca());
        assertEquals("Kicks", resultado.modelo());
        assertEquals("DEM-1234", resultado.placa());
        assertEquals(inicio.plusDays(3), resultado.fechaDevolucionPrevista());
        assertEquals(new BigDecimal("850.00"), resultado.tarifaDia());
        assertEquals(new BigDecimal("2550.00"), resultado.totalEstimado());
    }

    @Test void ordenaPorDevolucionYDesempataPorId() {
        reserva(3, "EN_CURSO", inicio.plusDays(4));
        reserva(2, "EN_CURSO", inicio.plusDays(3));
        reserva(1, "EN_CURSO", inicio.plusDays(3));
        assertEquals(java.util.List.of(1L, 2L, 3L), rentas.listar().stream()
                .map(r -> r.reservacionId()).toList());
    }

    @Test void noOcultaRentaVencidaNiClienteInactivo() {
        reserva(1, "EN_CURSO", inicio.plusDays(1));
        jdbc.update("UPDATE clientes SET activo = FALSE WHERE id = 1");
        // Una renta de octubre de 2026 debe seguir apareciendo, aun consultada años después.
        assertEquals(1, rentas.listar().size());
    }

    @Test void alFinalizarDejaDeAparecer() {
        reserva(1, "EN_CURSO", inicio.plusDays(3));
        jdbc.update("UPDATE reservaciones SET estado = 'FINALIZADA' WHERE id = 1");
        assertTrue(rentas.listar().isEmpty());
    }
}
