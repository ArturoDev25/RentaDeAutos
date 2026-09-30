package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.exception.TarifaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifaAltaServiceTest {
    @Mock private TarifaRepository tarifas;
    @Mock private CategoriaRepository categorias;
    @InjectMocks private TarifaService servicio;
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);

    private TarifaRequest datos(LocalDate fin) {
        return new TarifaRequest(2L, new BigDecimal("850.00"), BigDecimal.ZERO, inicio, fin);
    }

    @Test void crearBloqueaCategoriaAntesDeValidarYGuardar() {
        Categoria categoria = new Categoria();
        categoria.setNombre("SUV");
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(categoria));
        when(tarifas.buscarTraslapes(2L, inicio, null)).thenReturn(List.of());
        when(tarifas.saveAndFlush(any(Tarifa.class))).thenAnswer(i -> i.getArgument(0));
        var resultado = servicio.crear(datos(null));
        assertEquals(new BigDecimal("850.00"), resultado.precioDia());
        assertTrue(resultado.activo());
        assertNull(resultado.fechaFin());
        var orden = inOrder(categorias, tarifas);
        orden.verify(categorias).bloquearPorId(2L);
        orden.verify(tarifas).buscarTraslapes(2L, inicio, null);
        orden.verify(tarifas).saveAndFlush(any(Tarifa.class));
    }

    @Test void fechasInvertidasNoConsultanNiGuardan() {
        var error = assertThrows(TarifaException.class, () -> servicio.crear(datos(inicio.minusDays(1))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(categorias, tarifas);
    }

    @Test void categoriaInexistenteRechazaAlta() {
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.empty());
        assertThrows(TarifaException.class, () -> servicio.crear(datos(null)));
        verifyNoInteractions(tarifas);
    }

    @Test void categoriaInactivaRechazaAlta() {
        Categoria categoria = new Categoria();
        categoria.setActivo(false);
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(categoria));
        assertThrows(TarifaException.class, () -> servicio.crear(datos(null)));
        verifyNoInteractions(tarifas);
    }

    @Test void traslapeDevuelveConflictoSinGuardar() {
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(new Categoria()));
        when(tarifas.buscarTraslapes(2L, inicio, null)).thenReturn(List.of(new Tarifa()));
        var error = assertThrows(TarifaException.class, () -> servicio.crear(datos(null)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(tarifas, never()).saveAndFlush(any());
    }
}
