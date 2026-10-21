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
        assertFalse(v.tieneImagen());
    }

    @Test void indicaSiElVehiculoTieneFotoSinIncluirla() {
        jdbc.update("UPDATE vehiculos SET imagen = 'data:image/png;base64,AAAA' WHERE id = 1");
        assertTrue(catalogo.listar(ahora).get(0).tieneImagen());
        assertEquals("data:image/png;base64,AAAA", catalogo.imagen(1L).orElseThrow());
    }

    @Test void imagenVaciaSeTrataComoSinFoto() {
        jdbc.update("UPDATE vehiculos SET imagen = '' WHERE id = 1");
        assertFalse(catalogo.listar(ahora).get(0).tieneImagen());
        assertTrue(catalogo.imagen(1L).isEmpty());
    }

    @Test void imagenDeVehiculoInexistenteOdeBajaNoSeEntrega() {
        jdbc.update("UPDATE vehiculos SET imagen = 'data:image/png;base64,AAAA', estado = 'BAJA' WHERE id = 1");
        assertTrue(catalogo.imagen(1L).isEmpty());
        assertTrue(catalogo.imagen(999L).isEmpty());
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
    private void segundoVehiculo() {
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (2, 'Lujo', 0, TRUE)");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (2, 2, 'DEMO2', '2HGCM82633A004352', 'BMW', 'Serie 3', 2025, 0, 'DISPONIBLE')
                """);
        jdbc.update("""
                INSERT INTO tarifas (id, categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, activo)
                VALUES (2, 2, 1800, 100, '2026-10-16', TRUE)
                """);
    }

    @Test void filtraCategoriaSinMezclarVehiculos() {
        segundoVehiculo();
        assertEquals(java.util.List.of(1L), catalogo.listar(ahora, 1L, null, null)
                .stream().map(v -> v.id()).toList());
    }

    @Test void filtraPrecioMinimoInclusivo() {
        segundoVehiculo();
        assertEquals(java.util.List.of(2L), catalogo.listar(ahora, null, new BigDecimal("1800"), null)
                .stream().map(v -> v.id()).toList());
    }

    @Test void filtraPrecioMaximoInclusivo() {
        segundoVehiculo();
        assertEquals(java.util.List.of(1L), catalogo.listar(ahora, null, null, new BigDecimal("850"))
                .stream().map(v -> v.id()).toList());
    }

    @Test void filtrosCombinadosRespetanLosDosLimites() {
        segundoVehiculo();
        assertEquals(1, catalogo.listar(ahora, 1L, new BigDecimal("850"), new BigDecimal("850")).size());
        assertTrue(catalogo.listar(ahora, 2L, new BigDecimal("500"), new BigDecimal("1000")).isEmpty());
    }

    @Test void categoriaInexistenteDevuelveListaVacia() {
        assertTrue(catalogo.listar(ahora, 999L, null, null).isEmpty());
    }

    @Test void filtraTarifaVigenteSinUsarPreciosFuturos() {
        jdbc.update("""
                INSERT INTO tarifas (id, categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, activo)
                VALUES (2, 1, 100, 0, '2026-10-17', TRUE)
                """);
        assertTrue(catalogo.listar(ahora, null, null, new BigDecimal("500")).isEmpty());
    }

    @Test void filtrarNoVuelveOfertableUnAutoDeBaja() {
        jdbc.update("UPDATE vehiculos SET estado = 'BAJA'");
        assertTrue(catalogo.listar(ahora, 1L, BigDecimal.ZERO, new BigDecimal("1000")).isEmpty());
    }

    private void compromiso(String estado, LocalDateTime inicio, LocalDateTime fin) {
        jdbc.update("""
                INSERT INTO reservaciones (id, cliente_id, vehiculo_id, creado_por_id,
                    fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (1, 1, 1, 1, ?, ?, ?, 850, 850)
                """, inicio, fin, estado);
    }

    @ParameterizedTest @ValueSource(strings = {"PENDIENTE", "CONFIRMADA"})
    void compromisoActualImpideOferta(String estado) {
        compromiso(estado, ahora, ahora.plusDays(1));
        assertTrue(catalogo.listar(ahora).isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"CANCELADA", "FINALIZADA"})
    void compromisoCerradoNoImpideOferta(String estado) {
        compromiso(estado, ahora.minusHours(1), ahora.plusDays(1));
        assertEquals(1, catalogo.listar(ahora).size());
    }

    @Test void compromisoFuturoNoBloqueaConsultaDelMomentoActual() {
        compromiso("PENDIENTE", ahora.plusDays(1), ahora.plusDays(2));
        assertEquals(1, catalogo.listar(ahora).size());
    }

    @Test void compromisoQueTerminaAhoraNoBloqueaConsulta() {
        compromiso("CONFIRMADA", ahora.minusDays(1), ahora);
        assertEquals(1, catalogo.listar(ahora).size());
    }

}
