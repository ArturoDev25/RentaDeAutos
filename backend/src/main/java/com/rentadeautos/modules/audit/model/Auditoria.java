package com.rentadeautos.modules.audit.model;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id", nullable = true)
    private UsuarioApp usuario;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(nullable = false, length = 80)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 30)
    private String resultado;

    @Column(name = "valores_anteriores", columnDefinition = "JSON")
    private String valoresAnteriores;

    @Column(name = "valores_nuevos", columnDefinition = "JSON")
    private String valoresNuevos;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected Auditoria() {
    }

    public Auditoria(UsuarioApp usuario, String accion, String entidad,
                     Long entidadId, String resultado,
                     String valoresAnteriores, String valoresNuevos,
                     String direccionIp, LocalDateTime fechaHora) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.resultado = resultado;
        this.valoresAnteriores = valoresAnteriores;
        this.valoresNuevos = valoresNuevos;
        this.direccionIp = direccionIp;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public UsuarioApp getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getResultado() { return resultado; }
    public String getValoresAnteriores() { return valoresAnteriores; }
    public String getValoresNuevos() { return valoresNuevos; }
    public String getDireccionIp() { return direccionIp; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
