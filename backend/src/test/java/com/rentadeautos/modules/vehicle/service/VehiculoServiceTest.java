package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.FiltroVehiculos;
import com.rentadeautos.modules.vehicle.dto.VehiculoRequest;
import com.rentadeautos.modules.vehicle.dto.VehiculoResponse;
import com.rentadeautos.modules.vehicle.exception.VehiculoDuplicadoException;
import com.rentadeautos.modules.vehicle.exception.VehiculoInvalidoException;
import com.rentadeautos.modules.vehicle.exception.VehiculoNoEncontradoException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Comprueba las reglas de negocio de la gestión de vehículos (S2-06).
 */
@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    private static final String VIN = "3N1AB7AP0FY123456";

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private VehiculoService vehiculoService;

    private Categoria categoria(Long id, boolean activa) {
        Categoria categoria = new Categoria();
        ReflectionTestUtils.setField(categoria, "id", id);
        categoria.setNombre("Compacto");
        categoria.setDepositoBase(new BigDecimal("1000.00"));
        categoria.setActivo(activa);
        return categoria;
    }

    private Vehiculo vehiculo(Long id, String placa, String vin) {
        Vehiculo vehiculo = new Vehiculo();
        ReflectionTestUtils.setField(vehiculo, "id", id);
        vehiculo.setCategoria(categoria(1L, true));
        vehiculo.setPlaca(placa);
        vehiculo.setVin(vin);
        vehiculo.setMarca("Nissan");
        vehiculo.setModelo("Versa");
        vehiculo.setAnio(2022);
        vehiculo.setKilometraje(new BigDecimal("15000.0"));
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        return vehiculo;
    }

    private VehiculoRequest peticion(Long categoriaId, String placa, String vin, int anio) {
        return new VehiculoRequest(categoriaId, placa, vin, "  Nissan ", " Versa ",
                anio, "  ", new BigDecimal("15000.0"));
    }

    @Test
    @DisplayName("Crear guarda placa y VIN en mayúsculas y el vehículo inicia DISPONIBLE")
    void crearNormalizaEIniciaDisponible() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("FAK-1234")).thenReturn(false);
        when(vehiculoRepository.existsByVinIgnoreCase(VIN)).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, true)));
        when(vehiculoRepository.saveAndFlush(any(Vehiculo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        VehiculoResponse respuesta = vehiculoService.crear(
                peticion(1L, "  fak-1234 ", "3n1ab7ap0fy123456", 2022));

        assertEquals("FAK-1234", respuesta.placa());
        assertEquals(VIN, respuesta.vin());
        assertEquals("Nissan", respuesta.marca());
        assertEquals("Versa", respuesta.modelo());
        assertNull(respuesta.color());
        assertEquals(EstadoVehiculo.DISPONIBLE, respuesta.estado());
        assertEquals(1L, respuesta.categoriaId());
    }

    @Test
    @DisplayName("Crear con placa repetida se rechaza indicando la placa")
    void crearConPlacaDuplicada() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("FAK-1234")).thenReturn(true);

        VehiculoDuplicadoException error = assertThrows(VehiculoDuplicadoException.class,
                () -> vehiculoService.crear(peticion(1L, "FAK-1234", VIN, 2022)));

        assertEquals(VehiculoDuplicadoException.PLACA_DUPLICADA, error.getMessage());
        verify(vehiculoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con VIN repetido se rechaza indicando el VIN")
    void crearConVinDuplicado() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("FAK-1234")).thenReturn(false);
        when(vehiculoRepository.existsByVinIgnoreCase(VIN)).thenReturn(true);

        VehiculoDuplicadoException error = assertThrows(VehiculoDuplicadoException.class,
                () -> vehiculoService.crear(peticion(1L, "FAK-1234", VIN, 2022)));

        assertEquals(VehiculoDuplicadoException.VIN_DUPLICADO, error.getMessage());
        verify(vehiculoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con una categoría inexistente se rechaza")
    void crearConCategoriaInexistente() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("FAK-1234")).thenReturn(false);
        when(vehiculoRepository.existsByVinIgnoreCase(VIN)).thenReturn(false);
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(VehiculoInvalidoException.class,
                () -> vehiculoService.crear(peticion(9L, "FAK-1234", VIN, 2022)));

        verify(vehiculoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con una categoría inactiva se rechaza")
    void crearConCategoriaInactiva() {
        when(vehiculoRepository.existsByPlacaIgnoreCase("FAK-1234")).thenReturn(false);
        when(vehiculoRepository.existsByVinIgnoreCase(VIN)).thenReturn(false);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria(2L, false)));

        VehiculoInvalidoException error = assertThrows(VehiculoInvalidoException.class,
                () -> vehiculoService.crear(peticion(2L, "FAK-1234", VIN, 2022)));

        assertEquals("La categoría indicada está inactiva", error.getMessage());
    }

    @Test
    @DisplayName("Crear con un año posterior al siguiente se rechaza")
    void crearConAnioFuturo() {
        int anioInvalido = Year.now().getValue() + 2;

        assertThrows(VehiculoInvalidoException.class,
                () -> vehiculoService.crear(peticion(1L, "FAK-1234", VIN, anioInvalido)));

        verifyNoInteractions(vehiculoRepository, categoriaRepository);
    }

    @Test
    @DisplayName("Consultar un vehículo inexistente lanza no encontrado")
    void obtenerInexistente() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VehiculoNoEncontradoException.class,
                () -> vehiculoService.obtener(99L));
    }

    @Test
    @DisplayName("Editar conserva su propia placa y VIN sin marcarlos como duplicados")
    void editarConservaSusPropiosDatos() {
        when(vehiculoRepository.findById(1L))
                .thenReturn(Optional.of(vehiculo(1L, "FAK-1234", VIN)));
        when(vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot("FAK-1234", 1L))
                .thenReturn(false);
        when(vehiculoRepository.existsByVinIgnoreCaseAndIdNot(VIN, 1L)).thenReturn(false);
        when(vehiculoRepository.saveAndFlush(any(Vehiculo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        VehiculoRequest cambios = new VehiculoRequest(1L, "FAK-1234", VIN, "Nissan",
                "Versa Advance", 2023, "Rojo", new BigDecimal("16000.5"));

        VehiculoResponse respuesta = vehiculoService.editar(1L, cambios);

        assertEquals("Versa Advance", respuesta.modelo());
        assertEquals(2023, respuesta.anio());
        assertEquals("Rojo", respuesta.color());
        assertEquals(new BigDecimal("16000.5"), respuesta.kilometraje());
        // La categoría no cambió, así que no se vuelve a consultar.
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    @DisplayName("Editar con la placa de otro vehículo se rechaza")
    void editarConPlacaDeOtro() {
        when(vehiculoRepository.findById(1L))
                .thenReturn(Optional.of(vehiculo(1L, "FAK-1234", VIN)));
        when(vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot("XYZ-9876", 1L))
                .thenReturn(true);

        assertThrows(VehiculoDuplicadoException.class,
                () -> vehiculoService.editar(1L, peticion(1L, "XYZ-9876", VIN, 2022)));

        verify(vehiculoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Editar un vehículo inexistente lanza no encontrado")
    void editarInexistente() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VehiculoNoEncontradoException.class,
                () -> vehiculoService.editar(99L, peticion(1L, "FAK-1234", VIN, 2022)));
    }

    @Test
    @DisplayName("Se puede cambiar el estado de un vehículo")
    void cambiarEstado() {
        when(vehiculoRepository.findById(1L))
                .thenReturn(Optional.of(vehiculo(1L, "FAK-1234", VIN)));
        when(vehiculoRepository.saveAndFlush(any(Vehiculo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        VehiculoResponse respuesta =
                vehiculoService.cambiarEstado(1L, EstadoVehiculo.MANTENIMIENTO);

        assertEquals(EstadoVehiculo.MANTENIMIENTO, respuesta.estado());
    }

    @Test
    @DisplayName("Listar sin filtros no filtra nada")
    void listarSinFiltros() {
        when(vehiculoRepository.buscar(null, null, null, null, null))
                .thenReturn(List.of(vehiculo(1L, "FAK-1234", VIN)));

        List<VehiculoResponse> respuesta = vehiculoService.listar(FiltroVehiculos.sinFiltros());

        assertEquals(1, respuesta.size());
    }

    @Test
    @DisplayName("Listar aplica los filtros y convierte el texto en patrón de búsqueda")
    void listarConFiltros() {
        when(vehiculoRepository.buscar("%nissan%", EstadoVehiculo.DISPONIBLE, 1L, 2020, 2024))
                .thenReturn(List.of(vehiculo(1L, "FAK-1234", VIN)));

        List<VehiculoResponse> respuesta = vehiculoService.listar(new FiltroVehiculos(
                "  NISSAN ", EstadoVehiculo.DISPONIBLE, 1L, 2020, 2024));

        assertEquals(1, respuesta.size());
        assertEquals("FAK-1234", respuesta.get(0).placa());
    }

    @Test
    @DisplayName("Un texto de búsqueda en blanco se ignora")
    void listarConTextoEnBlanco() {
        when(vehiculoRepository.buscar(null, null, null, null, null)).thenReturn(List.of());

        vehiculoService.listar(new FiltroVehiculos("   ", null, null, null, null));

        verify(vehiculoRepository).buscar(null, null, null, null, null);
    }

    @Test
    @DisplayName("Un texto de búsqueda demasiado largo se rechaza")
    void listarConTextoLargo() {
        String largo = "a".repeat(VehiculoService.LONGITUD_MAXIMA_BUSQUEDA + 1);

        assertThrows(VehiculoInvalidoException.class,
                () -> vehiculoService.listar(new FiltroVehiculos(largo, null, null, null, null)));

        verifyNoInteractions(vehiculoRepository);
    }

    @Test
    @DisplayName("Un rango de años invertido se rechaza")
    void listarConRangoDeAniosInvertido() {
        assertThrows(VehiculoInvalidoException.class,
                () -> vehiculoService.listar(new FiltroVehiculos(null, null, null, 2024, 2020)));

        verifyNoInteractions(vehiculoRepository);
    }

    @Test
    @DisplayName("Los comodines del usuario se buscan como texto literal")
    void patronEscapaComodines() {
        assertEquals("%50!%!_a!!%", VehiculoService.patronBusqueda("50%_A!"));
        assertNull(VehiculoService.patronBusqueda(null));
    }
}
