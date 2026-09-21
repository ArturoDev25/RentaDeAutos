package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de las categorías de vehículos.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Categoria> findAllByOrderByNombreAsc();

    List<Categoria> findByActivoOrderByNombreAsc(Boolean activo);
}
