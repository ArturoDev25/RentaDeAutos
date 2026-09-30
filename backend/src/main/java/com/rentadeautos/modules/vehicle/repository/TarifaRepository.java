package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las tarifas del catálogo; las reglas de negocio irán en el servicio. */
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
}
