package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.CategoriaRequest;
import com.rentadeautos.modules.vehicle.dto.CategoriaResponse;
import com.rentadeautos.modules.vehicle.exception.CategoriaDuplicadaException;
import com.rentadeautos.modules.vehicle.exception.CategoriaNoEncontradaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprueba las reglas de negocio del catálogo de categorías (S2-08).
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        ReflectionTestUtils.setField(categoria, "id", id);
        categoria.setNombre(nombre);
        categoria.setDepositoBase(new BigDecimal("1000.00"));
        categoria.setActivo(true);
        return categoria;
    }

    @Test
    @DisplayName("Crear guarda la categoría con el nombre sin espacios sobrantes")
    void crearQuitaEspaciosYQuedaActiva() {
        when(categoriaRepository.existsByNombreIgnoreCase("SUV")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoriaResponse respuesta = categoriaService.crear(
                new CategoriaRequest("  SUV  ", "  Camionetas  ", new BigDecimal("3000.00")));

        assertEquals("SUV", respuesta.nombre());
        assertEquals("Camionetas", respuesta.descripcion());
        assertTrue(respuesta.activo());
    }

    @Test
    @DisplayName("Una descripción en blanco se guarda como nula")
    void crearConDescripcionEnBlancoGuardaNula() {
        when(categoriaRepository.existsByNombreIgnoreCase("SUV")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoriaResponse respuesta = categoriaService.crear(
                new CategoriaRequest("SUV", "   ", BigDecimal.ZERO));

        assertNull(respuesta.descripcion());
    }

    @Test
    @DisplayName("No se puede crear una categoría con nombre repetido")
    void crearConNombreRepetidoLanzaExcepcion() {
        when(categoriaRepository.existsByNombreIgnoreCase("SUV")).thenReturn(true);

        assertThrows(CategoriaDuplicadaException.class, () ->
                categoriaService.crear(new CategoriaRequest("SUV", null, BigDecimal.ZERO)));

        verify(categoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Editar actualiza los datos de la categoría")
    void editarActualizaDatos() {
        Categoria existente = categoria(1L, "Compacto");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Económico", 1L))
                .thenReturn(false);
        when(categoriaRepository.saveAndFlush(existente)).thenReturn(existente);

        CategoriaResponse respuesta = categoriaService.editar(
                1L, new CategoriaRequest("Económico", "Autos pequeños", new BigDecimal("500.00")));

        assertEquals("Económico", respuesta.nombre());
        assertEquals(new BigDecimal("500.00"), respuesta.depositoBase());
    }

    @Test
    @DisplayName("No se puede renombrar con el nombre de otra categoría")
    void editarConNombreDeOtraCategoriaLanzaExcepcion() {
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria(1L, "Compacto")));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("SUV", 1L))
                .thenReturn(true);

        assertThrows(CategoriaDuplicadaException.class, () ->
                categoriaService.editar(1L, new CategoriaRequest("SUV", null, BigDecimal.ZERO)));

        verify(categoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Editar una categoría inexistente lanza excepción")
    void editarInexistenteLanzaExcepcion() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoriaNoEncontradaException.class, () ->
                categoriaService.editar(99L, new CategoriaRequest("SUV", null, BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("Una categoría se puede desactivar sin eliminarla")
    void cambiarEstadoDesactivaSinEliminar() {
        Categoria existente = categoria(1L, "SUV");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponse respuesta = categoriaService.cambiarEstado(1L, false);

        assertFalse(respuesta.activo());
        verify(categoriaRepository, never()).delete(any());
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Cambiar estado de una categoría inexistente lanza excepción")
    void cambiarEstadoInexistenteLanzaExcepcion() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoriaNoEncontradaException.class,
                () -> categoriaService.cambiarEstado(99L, true));
    }

    @Test
    @DisplayName("Listar sin filtro devuelve todas las categorías")
    void listarSinFiltroDevuelveTodas() {
        when(categoriaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(categoria(1L, "SUV"), categoria(2L, "Sedán")));

        assertEquals(2, categoriaService.listar(null).size());
        verify(categoriaRepository, never()).findByActivoOrderByNombreAsc(any());
    }

    @Test
    @DisplayName("Listar con filtro devuelve solo las del estado pedido")
    void listarConFiltroUsaElEstado() {
        when(categoriaRepository.findByActivoOrderByNombreAsc(false))
                .thenReturn(List.of());

        assertTrue(categoriaService.listar(false).isEmpty());
        verify(categoriaRepository, never()).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("Consultar una categoría inexistente lanza excepción")
    void obtenerInexistenteLanzaExcepcion() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoriaNoEncontradaException.class,
                () -> categoriaService.obtener(99L));
    }
}
