package com.rentadeautos.modules.rental.repository;

import com.rentadeautos.modules.rental.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    boolean existsByReservacionId(Long reservacionId);

    Optional<Entrega> findByReservacionId(Long reservacionId);
}
