package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.CategoriaRequest;
import com.rentadeautos.modules.vehicle.dto.CategoriaResponse;
import com.rentadeautos.modules.vehicle.exception.CategoriaDuplicadaException;
import com.rentadeautos.modules.vehicle.exception.CategoriaNoEncontradaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio del catálogo de categorías de vehículos.
 * Las categorías nunca se eliminan: solo se activan o desactivan.
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Boolean activo) {
        List<Categoria> categorias = (activo == null)
                ? categoriaRepository.findAllByOrderByNombreAsc()
                : categoriaRepository.findByActivoOrderByNombreAsc(activo);

        return categorias.stream().map(CategoriaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.desde(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest peticion) {
        String nombre = peticion.nombre().strip();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new CategoriaDuplicadaException();
        }

        Categoria categoria = new Categoria();
        aplicarDatos(categoria, peticion, nombre);

        return CategoriaResponse.desde(categoriaRepository.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaResponse editar(Long id, CategoriaRequest peticion) {
        Categoria categoria = buscar(id);
        String nombre = peticion.nombre().strip();

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new CategoriaDuplicadaException();
        }

        aplicarDatos(categoria, peticion, nombre);

        return CategoriaResponse.desde(categoriaRepository.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaResponse cambiarEstado(Long id, boolean activo) {
        Categoria categoria = buscar(id);
        categoria.setActivo(activo);

        return CategoriaResponse.desde(categoriaRepository.save(categoria));
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private void aplicarDatos(Categoria categoria, CategoriaRequest peticion, String nombre) {
        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiarTexto(peticion.descripcion()));
        categoria.setDepositoBase(peticion.depositoBase());
    }

    /** Una descripción vacía o solo con espacios se guarda como nula. */
    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
