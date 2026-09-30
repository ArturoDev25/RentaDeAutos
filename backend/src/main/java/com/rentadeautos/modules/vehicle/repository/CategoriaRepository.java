package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de las categorías de vehículos.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Serializa los cambios de tarifas incluso cuando aún no hay ninguna. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Categoria c where c.id = :id")
    Optional<Categoria> bloquearPorId(@Param("id") Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Categoria> findAllByOrderByNombreAsc();

    List<Categoria> findByActivoOrderByNombreAsc(Boolean activo);
}
