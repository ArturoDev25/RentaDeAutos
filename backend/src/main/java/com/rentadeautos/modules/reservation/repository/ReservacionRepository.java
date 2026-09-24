package com.rentadeautos.modules.reservation.repository;

import com.rentadeautos.modules.reservation.model.Reservacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {
    List<Reservacion> findAllByOrderByFechaInicioDesc();
}
