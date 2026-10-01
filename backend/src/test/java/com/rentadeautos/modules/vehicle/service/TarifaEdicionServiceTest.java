package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.exception.TarifaException;
import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifaEdicionServiceTest {
    @Mock private TarifaRepository tarifas;
    @Mock private CategoriaRepository categorias;
    @InjectMocks private TarifaService servicio;
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);

    private Categoria categoria(Long id) {
        Categoria c = new Categoria();
        ReflectionTestUtils.setField(c, "id", id);
        c.setNombre("Categoría " + id);
        return c;
    }

    private Tarifa tarifa(Categoria c) {
        Tarifa t = new Tarifa();
        ReflectionTestUtils.setField(t, "id", 7L);
        t.setCategoria(c);
        t.setPrecioDia(new BigDecimal("850.00"));
        t.setFechaInicio(inicio);
        return t;
    }

    private TarifaRequest datos(Long categoriaId) {
        return new TarifaRequest(categoriaId, new BigDecimal("900.00"), BigDecimal.TEN, inicio, null);
    }

    @Test void editaExcluyendoSuPropioId() {
        Categoria c = categoria(2L);
        Tarifa t = tarifa(c);
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(2L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(c));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(t));
        when(tarifas.buscarTraslapesExcluyendo(2L, inicio, null, 7L)).thenReturn(List.of());
        when(tarifas.saveAndFlush(t)).thenReturn(t);
        var resultado = servicio.editar(7L, datos(2L));
        assertEquals(7L, resultado.id());
        assertEquals(new BigDecimal("900.00"), resultado.precioDia());
        assertEquals(BigDecimal.TEN, resultado.cargoAtrasoDia());
        verify(tarifas).buscarTraslapesExcluyendo(2L, inicio, null, 7L);
    }

    @Test void cambiarCategoriaBloqueaEnOrdenYValidaDestino() {
        Categoria origen = categoria(3L), destino = categoria(2L);
        Tarifa t = tarifa(origen);
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(3L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(destino));
        when(categorias.bloquearPorId(3L)).thenReturn(Optional.of(origen));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(t));
        when(tarifas.buscarTraslapesExcluyendo(2L, inicio, null, 7L)).thenReturn(List.of());
        when(tarifas.saveAndFlush(t)).thenReturn(t);
        assertEquals(2L, servicio.editar(7L, datos(2L)).categoriaId());
        var orden = inOrder(categorias, tarifas);
        orden.verify(tarifas).consultarCategoriaId(7L);
        orden.verify(categorias).bloquearPorId(2L);
        orden.verify(categorias).bloquearPorId(3L);
        orden.verify(tarifas).bloquearPorId(7L);
    }

    @Test void inexistenteNoGuarda() {
        when(tarifas.consultarCategoriaId(99L)).thenReturn(Optional.empty());
        assertThrows(TarifaNoEncontradaException.class, () -> servicio.editar(99L, datos(2L)));
        verifyNoInteractions(categorias);
        verify(tarifas, never()).saveAndFlush(any());
    }

    @Test void traslapeNoModificaPrecio() {
        Categoria c = categoria(2L);
        Tarifa t = tarifa(c);
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(2L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(c));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(t));
        when(tarifas.buscarTraslapesExcluyendo(2L, inicio, null, 7L)).thenReturn(List.of(new Tarifa()));
        assertThrows(TarifaException.class, () -> servicio.editar(7L, datos(2L)));
        assertEquals(new BigDecimal("850.00"), t.getPrecioDia());
        verify(tarifas, never()).saveAndFlush(any());
    }

    @Test void fechasInvertidasNoConsultan() {
        var datos = new TarifaRequest(2L, BigDecimal.TEN, BigDecimal.ZERO, inicio, inicio.minusDays(1));
        assertThrows(TarifaException.class, () -> servicio.editar(7L, datos));
        verifyNoInteractions(tarifas, categorias);
    }

    @Test void editarInactivaNoLaReactivaNiBloqueaVigencia() {
        Categoria c = categoria(2L);
        Tarifa t = tarifa(c);
        t.setActivo(false);
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(2L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(c));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(t));
        when(tarifas.saveAndFlush(t)).thenReturn(t);
        assertFalse(servicio.editar(7L, datos(2L)).activo());
        verify(tarifas, never()).buscarTraslapesExcluyendo(any(), any(), any(), any());
    }

    @Test void cambioConcurrenteDeCategoriaDevuelveConflicto() {
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(2L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(categoria(2L)));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(tarifa(categoria(3L))));
        assertThrows(TarifaException.class, () -> servicio.editar(7L, datos(2L)));
        verify(tarifas, never()).saveAndFlush(any());
    }
}
