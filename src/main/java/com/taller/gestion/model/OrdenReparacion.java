package com.taller.gestion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orden_reparacion")
public class OrdenReparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long idOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    @Column(name = "descripcion_problema", length = 1000)
    private String descripcionProblema;

    @Column(name = "km_entrada")
    private Integer kmEntrada;

    // Identificador publico para el portal de seguimiento (sin login): quien lo
    // conoce puede ver el estado de la orden y responder al presupuesto.
    // Nullable a proposito: ver el comentario de Usuario.rol (ORA-01758 en Oracle
    // al añadir una columna NOT NULL a una tabla con filas). Oracle sí admite
    // varios NULL en una columna UNIQUE, así que la restriccion unica es segura.
    @Column(name = "codigo_seguimiento", unique = true, length = 36)
    private String codigoSeguimiento;

    // null = pendiente de respuesta del cliente. Solo se responde desde el portal publico.
    @Column(name = "presupuesto_aprobado")
    private Boolean presupuestoAprobado;

    @Column(name = "presupuesto_respondido_en")
    private LocalDateTime presupuestoRespondidoEn;

    // Las lineas son parte del agregado: se guardan y borran EN CASCADA con la orden.
    // orphanRemoval = true -> quitar una linea de la lista la borra de la BD.
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaOrden> lineas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaEntrada == null) {
            fechaEntrada = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "RECEPCION";
        }
        if (codigoSeguimiento == null) {
            codigoSeguimiento = UUID.randomUUID().toString();
        }
    }

    // Metodos que mantienen sincronizados los dos lados de la relacion
    public void addLinea(LineaOrden linea) {
        lineas.add(linea);
        linea.setOrden(this);
    }

    public void removeLinea(LineaOrden linea) {
        lineas.remove(linea);
        linea.setOrden(null);
    }

    public Long getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(Long idOrden) {
        this.idOrden = idOrden;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public LocalDateTime getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDateTime fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    public void setDescripcionProblema(String descripcionProblema) {
        this.descripcionProblema = descripcionProblema;
    }

    public Integer getKmEntrada() {
        return kmEntrada;
    }

    public void setKmEntrada(Integer kmEntrada) {
        this.kmEntrada = kmEntrada;
    }

    public List<LineaOrden> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaOrden> lineas) {
        this.lineas = lineas;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public void setCodigoSeguimiento(String codigoSeguimiento) {
        this.codigoSeguimiento = codigoSeguimiento;
    }

    public Boolean getPresupuestoAprobado() {
        return presupuestoAprobado;
    }

    public void setPresupuestoAprobado(Boolean presupuestoAprobado) {
        this.presupuestoAprobado = presupuestoAprobado;
    }

    public LocalDateTime getPresupuestoRespondidoEn() {
        return presupuestoRespondidoEn;
    }

    public void setPresupuestoRespondidoEn(LocalDateTime presupuestoRespondidoEn) {
        this.presupuestoRespondidoEn = presupuestoRespondidoEn;
    }
}
