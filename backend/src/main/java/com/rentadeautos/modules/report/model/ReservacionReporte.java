package com.rentadeautos.modules.report.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Vista de solo lectura de la tabla reservaciones, usada por el módulo de
 * reportes (S2-17). No inserta ni actualiza: esa responsabilidad es del
 * módulo Reservation (S2-16).
 */
@Entity
@Table(name = "reservaciones")
public class ReservacionReporte {

    @Id
    private Long id;

    private String estado;

    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }
}
