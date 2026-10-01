package com.rentadeautos.modules.rental.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrega física de un vehículo al cliente (S3-09).
 * Espeja la tabla entregas de V1__init_schema.sql: máximo una entrega por reservación.
 * Las columnas created_at y updated_at las llena MySQL.
 */
@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservacion_id", nullable = false, unique = true)
    private Long reservacionId;

    @Column(name = "entregado_por_id", nullable = false)
    private Long entregadoPorId;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "kilometraje_salida", nullable = false, precision = 10, scale = 1)
    private BigDecimal kilometrajeSalida;

    @Column(name = "combustible_salida", nullable = false, precision = 5, scale = 2)
    private BigDecimal combustibleSalida;

    @Column(name = "condicion_salida", nullable = false, columnDefinition = "TEXT")
    private String condicionSalida;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public Long getId() { return id; }
    public Long getReservacionId() { return reservacionId; }
    public void setReservacionId(Long reservacionId) { this.reservacionId = reservacionId; }
    public Long getEntregadoPorId() { return entregadoPorId; }
    public void setEntregadoPorId(Long entregadoPorId) { this.entregadoPorId = entregadoPorId; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public BigDecimal getKilometrajeSalida() { return kilometrajeSalida; }
    public void setKilometrajeSalida(BigDecimal kilometrajeSalida) { this.kilometrajeSalida = kilometrajeSalida; }
    public BigDecimal getCombustibleSalida() { return combustibleSalida; }
    public void setCombustibleSalida(BigDecimal combustibleSalida) { this.combustibleSalida = combustibleSalida; }
    public String getCondicionSalida() { return condicionSalida; }
    public void setCondicionSalida(String condicionSalida) { this.condicionSalida = condicionSalida; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
