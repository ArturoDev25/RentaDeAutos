package com.rentadeautos.modules.catalog.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import(CatalogoRepository.class)
class CatalogoRepositoryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogoRepository catalogo;
    private final LocalDateTime ahora = LocalDateTime.of(2026, 10, 16, 12, 0);

    @BeforeEach void preparar() {
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (1, 'SUV', 0, TRUE)");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (1, 1, 'DEMO1', '1HGCM82633A004352', 'Nissan', 'Kicks', 2024, 1000, 'DISPONIBLE')
                """);
        jdbc.update("""
                INSERT INTO tarifas (id, categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, fecha_fin, activo)
                VALUES (1, 1, 850, 100, '2026-10-16', '2026-10-16', TRUE)
                """);
    }

    @Test void devuelveDatosPublicosYTarifaEnLimitesInclusivos() {
        var lista = catalogo.listar(ahora);
        assertEquals(1, lista.size());
        var v = lista.get(0);
        assertEquals(1L, v.id());
        assertEquals("Nissan", v.marca());
        assertEquals("Kicks", v.modelo());
        assertEquals(2024, v.anio());
        assertEquals(1L, v.categoriaId());
        assertEquals("SUV", v.categoria());
        assertEquals(new BigDecimal("850.00"), v.tarifaDia());
    }

    @ParameterizedTest @ValueSource(strings = {"RESERVADO", "RENTADO", "MANTENIMIENTO", "BAJA"})
    void excluyeEstadosNoDisponibles(String estado) {
        jdbc.update("UPDATE vehiculos SET estado = ?", estado);
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void excluyeCategoriaInactiva() {
        jdbc.update("UPDATE categorias SET activo = FALSE");
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void excluyeTarifaInactiva() {
        jdbc.update("UPDATE tarifas SET activo = FALSE");
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void excluyeTarifasFueraDeVigencia() {
        assertTrue(catalogo.listar(ahora.minusDays(1)).isEmpty());
        assertTrue(catalogo.listar(ahora.plusDays(1)).isEmpty());
    }

    @Test void admiteVigenciaSinFin() {
        jdbc.update("UPDATE tarifas SET fecha_fin = NULL");
        assertEquals(1, catalogo.listar(ahora.plusYears(1)).size());
    }

    @Test void sinTarifaNoOfreceVehiculo() {
        jdbc.update("DELETE FROM tarifas");
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void traslapeNoDuplicaNiEscogeUnPrecioArbitrario() {
        jdbc.update("""
                INSERT INTO tarifas (id, categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, activo)
                VALUES (2, 1, 900, 100, '2026-10-16', TRUE)
                """);
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void catalogoVacioDevuelveListaVacia() {
        jdbc.update("DELETE FROM vehiculos");
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @Test void rentaEnCursoImpideOfertaAunqueElEstadoDelAutoSeaInconsistente() {
        jdbc.update("""
                INSERT INTO reservaciones (id, cliente_id, vehiculo_id, creado_por_id,
                    fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (1, 1, 1, 1, '2026-10-01 10:00:00', '2026-10-02 10:00:00', 'EN_CURSO', 850, 850)
                """);
        assertTrue(catalogo.listar(ahora).isEmpty());
    }
}
