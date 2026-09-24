package com.rentadeautos.modules.vehicle.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reglas de negocio de la gestión de vehículos (S2-06).
 *
 * <ul>
 *   <li>La placa y el VIN son únicos (RN-01) y se guardan en mayúsculas.</li>
 *   <li>Un vehículo nuevo siempre inicia en estado DISPONIBLE.</li>
 *   <li>El año va de 2000 al año actual + 1.</li>
 *   <li>Los vehículos no se eliminan: se cambian a estado BAJA.</li>
 * </ul>
 */
@Service
public class VehiculoService {

    static final int ANIO_MINIMO = 2000;

    private final VehiculoRepository vehiculoRepository;
    private final CategoriaRepository categoriaRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository,
                           CategoriaRepository categoriaRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(EstadoVehiculo estado, Long categoriaId) {
        return vehiculoRepository.buscar(estado, categoriaId).stream()
                .map(VehiculoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Long id) {
        return VehiculoResponse.desde(buscar(id));
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest peticion) {
        String placa = normalizarClave(peticion.placa());
        String vin = normalizarClave(peticion.vin());

        validarAnio(peticion.anio());

        if (vehiculoRepository.existsByPlacaIgnoreCase(placa)) {
            throw VehiculoDuplicadoException.placa();
        }
        if (vehiculoRepository.existsByVinIgnoreCase(vin)) {
            throw VehiculoDuplicadoException.vin();
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCategoria(buscarCategoriaActiva(peticion.categoriaId()));
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        aplicarDatos(vehiculo, peticion, placa, vin);

        return VehiculoResponse.desde(vehiculoRepository.saveAndFlush(vehiculo));
    }

    @Transactional
    public VehiculoResponse editar(Long id, VehiculoRequest peticion) {
        Vehiculo vehiculo = buscar(id);
        String placa = normalizarClave(peticion.placa());
        String vin = normalizarClave(peticion.vin());

        validarAnio(peticion.anio());

        if (vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, id)) {
            throw VehiculoDuplicadoException.placa();
        }
        if (vehiculoRepository.existsByVinIgnoreCaseAndIdNot(vin, id)) {
            throw VehiculoDuplicadoException.vin();
        }

        // Solo se exige categoría activa si se cambia a otra distinta.
        if (!Objects.equals(vehiculo.getCategoria().getId(), peticion.categoriaId())) {
            vehiculo.setCategoria(buscarCategoriaActiva(peticion.categoriaId()));
        }

        aplicarDatos(vehiculo, peticion, placa, vin);

        return VehiculoResponse.desde(vehiculoRepository.saveAndFlush(vehiculo));
    }

    @Transactional
    public VehiculoResponse cambiarEstado(Long id, EstadoVehiculo estado) {
        Vehiculo vehiculo = buscar(id);
        vehiculo.setEstado(estado);

        return VehiculoResponse.desde(vehiculoRepository.saveAndFlush(vehiculo));
    }

    private Vehiculo buscar(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(VehiculoNoEncontradoException::new);
    }

    private Categoria buscarCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new VehiculoInvalidoException(
                        "La categoría indicada no existe"));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new VehiculoInvalidoException("La categoría indicada está inactiva");
        }
        return categoria;
    }

    private void validarAnio(Integer anio) {
        int anioMaximo = Year.now().getValue() + 1;
        if (anio < ANIO_MINIMO || anio > anioMaximo) {
            throw new VehiculoInvalidoException(
                    "El año debe estar entre " + ANIO_MINIMO + " y " + anioMaximo);
        }
    }

    private void aplicarDatos(Vehiculo vehiculo, VehiculoRequest peticion,
                              String placa, String vin) {
        vehiculo.setPlaca(placa);
        vehiculo.setVin(vin);
        vehiculo.setMarca(peticion.marca().strip());
        vehiculo.setModelo(peticion.modelo().strip());
        vehiculo.setAnio(peticion.anio());
        vehiculo.setColor(limpiarTexto(peticion.color()));
        vehiculo.setKilometraje(peticion.kilometraje());
    }

    /** Placa y VIN se guardan sin espacios sobrantes y en mayúsculas. */
    private String normalizarClave(String valor) {
        return valor.strip().toUpperCase(Locale.ROOT);
    }

    /** Un texto opcional vacío o solo con espacios se guarda como nulo. */
    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
