package com.rentadeautos.modules.client.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String apellidos;

    @Column(length = 150, unique = true)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(name = "numero_licencia", nullable = false, length = 50, unique = true)
    private String numeroLicencia;

    @Column(name = "licencia_vencimiento", nullable = false)
    private LocalDate licenciaVencimiento;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getNumeroLicencia() { return numeroLicencia; }
    public void setNumeroLicencia(String numeroLicencia) { this.numeroLicencia = numeroLicencia; }
    public LocalDate getLicenciaVencimiento() { return licenciaVencimiento; }
    public void setLicenciaVencimiento(LocalDate licenciaVencimiento) { this.licenciaVencimiento = licenciaVencimiento; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}