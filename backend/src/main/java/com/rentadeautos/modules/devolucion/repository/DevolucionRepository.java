package com.rentadeautos.modules.devolucion.repository;

import com.rentadeautos.modules.devolucion.model.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de devoluciones (S3-10).
 * Extiende JpaRepository para obtener las operaciones CRUD estándar.
 */
public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    /** Verifica si una entrega ya tiene devolución registrada (evitar doble devolución). */
    boolean existsByEntregaId(Long entregaId);

    /** Recupera la devolución asociada a una entrega específica. */
    Optional<Devolucion> findByEntregaId(Long entregaId);
}
