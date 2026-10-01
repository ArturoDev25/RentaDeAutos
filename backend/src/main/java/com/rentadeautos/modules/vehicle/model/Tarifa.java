package com.rentadeautos.modules.vehicle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tarifa por categoría y periodo; corresponde a la tabla tarifas de V1. */
@Entity
@Table(name = "tarifas")
public class Tarifa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "precio_dia", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioDia;

    @Column(name = "cargo_atraso_dia", nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoAtrasoDia = BigDecimal.ZERO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Fin inclusivo; null significa vigencia sin fecha de término. */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public BigDecimal getPrecioDia() { return precioDia; }
    public void setPrecioDia(BigDecimal precioDia) { this.precioDia = precioDia; }
    public BigDecimal getCargoAtrasoDia() { return cargoAtrasoDia; }
    public void setCargoAtrasoDia(BigDecimal cargoAtrasoDia) { this.cargoAtrasoDia = cargoAtrasoDia; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
