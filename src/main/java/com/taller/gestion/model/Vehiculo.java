package com.taller.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Long idVehiculo;

    // Lado propietario de la relación: aquí vive la clave foránea (id_cliente).
    // LAZY = no carga el cliente hasta que se pide (evita traer datos de más).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotBlank
    @Size(max = 15)
    @Column(name = "matricula", nullable = false, unique = true, length = 15)
    private String matricula;

    @NotBlank
    @Size(max = 50)
    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @NotBlank
    @Size(max = 50)
    @Column(name = "modelo", nullable = false, length = 50)
    private String modelo;

    @Column(name = "anio")
    private Short anio;

    @Size(max = 20)
    @Column(name = "bastidor_vin", length = 20)
    private String bastidorVin;

    @Column(name = "km_actual")
    private Integer kmActual;

    @Column(name = "tipo", nullable = false, length = 12)
    private String tipo;

    public Vehiculo() {
    }

    // Getters y setters
    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Short getAnio() {
        return anio;
    }

    public void setAnio(Short anio) {
        this.anio = anio;
    }

    public String getBastidorVin() {
        return bastidorVin;
    }

    public void setBastidorVin(String bastidorVin) {
        this.bastidorVin = bastidorVin;
    }

    public Integer getKmActual() {
        return kmActual;
    }

    public void setKmActual(Integer kmActual) {
        this.kmActual = kmActual;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
