package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;

/** Acceso a las tarifas del catálogo; las reglas de negocio irán en el servicio. */
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    /** Límites inclusivos y lectura actual con bloqueo, también bajo MySQL REPEATABLE READ. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Tarifa t
            where t.categoria.id = :categoriaId and t.activo = true
              and (:hasta is null or t.fechaInicio <= :hasta)
              and (t.fechaFin is null or t.fechaFin >= :desde)
            """)
    List<Tarifa> buscarTraslapes(@Param("categoriaId") Long categoriaId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

}
