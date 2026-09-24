package com.rentadeautos.modules.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservaciones")
public class Reservacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;
    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";
    @Column(name = "tarifa_dia", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaDia;
    @Column(name = "total_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEstimado;
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
    public Long getCreadoPorId() { return creadoPorId; }
    public void setCreadoPorId(Long creadoPorId) { this.creadoPorId = creadoPorId; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTarifaDia() { return tarifaDia; }
    public void setTarifaDia(BigDecimal tarifaDia) { this.tarifaDia = tarifaDia; }
    public BigDecimal getTotalEstimado() { return totalEstimado; }
    public void setTotalEstimado(BigDecimal totalEstimado) { this.totalEstimado = totalEstimado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
