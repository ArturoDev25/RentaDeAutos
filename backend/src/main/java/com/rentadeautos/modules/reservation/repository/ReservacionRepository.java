package com.rentadeautos.modules.reservation.repository;

import com.rentadeautos.modules.reservation.model.Reservacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {
    List<Reservacion> findAllByOrderByFechaInicioDesc();

    /** S3-09: solo el vehículo, para bloquearlo antes que la reservación. */
    @Query("SELECT r.vehiculoId FROM Reservacion r WHERE r.id = :id")
    Optional<Long> findVehiculoIdById(@Param("id") Long id);

    /** S3-09: lectura actual con bloqueo (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservacion r WHERE r.id = :id")
    Optional<Reservacion> findByIdParaActualizar(@Param("id") Long id);

    /** S3-09: otra renta EN_CURSO del mismo vehículo impide una nueva entrega. */
    boolean existsByVehiculoIdAndEstadoAndIdNot(Long vehiculoId, String estado, Long id);
}
