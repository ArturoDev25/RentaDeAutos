package com.rentadeautos.modules.report.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Vista de solo lectura de la tabla clientes, usada por el módulo de
 * reportes (S2-17). No inserta ni actualiza: esa responsabilidad es del
 * módulo Client (S2-04).
 */
@Entity
@Table(name = "clientes")
public class ClienteReporte {

    @Id
    private Long id;

    private Boolean activo;

    public Long getId() {
        return id;
    }

    public Boolean getActivo() {
        return activo;
    }
}
