package com.taller.gestion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Cita de agenda. El dia de la cita, el taller pulsa "Convertir en orden" en
// el frontend, que abre crear.html con el vehiculo ya seleccionado; al guardar
// esa orden nueva, se enlaza aqui (idOrdenGenerada) y la cita pasa a COMPLETADA,
// para que ambos lados de la agenda queden consistentes entre si.
@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long idCita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "motivo", length = 200)
    private String motivo;

    // PENDIENTE, CONFIRMADA, CANCELADA o COMPLETADA.
    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    // null = todavia no se ha convertido en una orden de reparacion.
    // Se guarda como id simple (no relacion JPA): solo se usa para enlazar,
    // nunca se navega desde aqui a la orden en el backend.
    @Column(name = "id_orden_generada")
    private Long idOrdenGenerada;

    public Cita() {
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    public Long getIdCita() {
        return idCita;
    }

    public void setIdCita(Long idCita) {
        this.idCita = idCita;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getIdOrdenGenerada() {
        return idOrdenGenerada;
    }

    public void setIdOrdenGenerada(Long idOrdenGenerada) {
        this.idOrdenGenerada = idOrdenGenerada;
    }
}
