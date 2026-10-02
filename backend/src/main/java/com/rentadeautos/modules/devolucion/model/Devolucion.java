package com.rentadeautos.modules.devolucion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Devolución del vehículo y cálculo final (S3-10).
 * Espeja la tabla devoluciones definida en V1__init_schema.sql:
 *   máximo una devolución por entrega (uq_devoluciones_entrega).
 * Las columnas created_at y updated_at las gestiona MySQL.
 */
@Entity
@Table(name = "devoluciones")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → entregas.id; unicidad garantizada por uq_devoluciones_entrega. */
    @Column(name = "entrega_id", nullable = false, unique = true)
    private Long entregaId;

    /** Usuario que recibe el vehículo (agente / supervisor / admin). */
    @Column(name = "recibido_por_id", nullable = false)
    private Long recibidoPorId;

    @Column(name = "fecha_devolucion", nullable = false)
    private LocalDateTime fechaDevolucion;

    @Column(name = "kilometraje_entrada", nullable = false, precision = 10, scale = 1)
    private BigDecimal kilometrajeEntrada;

    /** Nivel de combustible al momento de la devolución (0–100). */
    @Column(name = "combustible_entrada", nullable = false, precision = 5, scale = 2)
    private BigDecimal combustibleEntrada;

    @Column(name = "condicion_entrada", nullable = false, columnDefinition = "TEXT")
    private String condicionEntrada;

    /** Recargo por devolución tardía (≥ 0). */
    @Column(name = "cargo_atraso", nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoAtraso = BigDecimal.ZERO;

    /** Cargos por daños declarados o detectados (≥ 0). */
    @Column(name = "cargo_danos", nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoDanos = BigDecimal.ZERO;

    /** Importe definitivo = subtotal base + cargo_atraso + cargo_danos. */
    @Column(name = "total_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFinal;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // ── Getters y setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getEntregaId() { return entregaId; }
    public void setEntregaId(Long entregaId) { this.entregaId = entregaId; }

    public Long getRecibidoPorId() { return recibidoPorId; }
    public void setRecibidoPorId(Long recibidoPorId) { this.recibidoPorId = recibidoPorId; }

    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }

    public BigDecimal getKilometrajeEntrada() { return kilometrajeEntrada; }
    public void setKilometrajeEntrada(BigDecimal kilometrajeEntrada) { this.kilometrajeEntrada = kilometrajeEntrada; }

    public BigDecimal getCombustibleEntrada() { return combustibleEntrada; }
    public void setCombustibleEntrada(BigDecimal combustibleEntrada) { this.combustibleEntrada = combustibleEntrada; }

    public String getCondicionEntrada() { return condicionEntrada; }
    public void setCondicionEntrada(String condicionEntrada) { this.condicionEntrada = condicionEntrada; }

    public BigDecimal getCargoAtraso() { return cargoAtraso; }
    public void setCargoAtraso(BigDecimal cargoAtraso) { this.cargoAtraso = cargoAtraso; }

    public BigDecimal getCargoDanos() { return cargoDanos; }
    public void setCargoDanos(BigDecimal cargoDanos) { this.cargoDanos = cargoDanos; }

    public BigDecimal getTotalFinal() { return totalFinal; }
    public void setTotalFinal(BigDecimal totalFinal) { this.totalFinal = totalFinal; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
