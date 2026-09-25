package com.rentadeautos.modules.report.model;

import com.rentadeautos.modules.vehicle.model.Categoria;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Vista de solo lectura de la tabla vehiculos, usada por el módulo de
 * reportes (S2-17). No inserta ni actualiza: esa responsabilidad es del
 * módulo Vehicle (S2-06).
 */
@Entity
@Table(name = "vehiculos")
public class VehiculoReporte {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", insertable = false, updatable = false)
    private Categoria categoria;

    private String estado;

    public Long getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getEstado() {
        return estado;
    }
}
