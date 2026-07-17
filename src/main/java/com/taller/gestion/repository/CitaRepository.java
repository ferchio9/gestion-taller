package com.taller.gestion.repository;

import com.taller.gestion.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByVehiculo_IdVehiculo(Long idVehiculo);

    // JOIN FETCH evita el N+1 de listar(): toResponse() navega cita -> vehiculo -> cliente.
    @Query("SELECT c FROM Cita c JOIN FETCH c.vehiculo v JOIN FETCH v.cliente")
    List<Cita> findAllConVehiculoYCliente();

    @Query("SELECT c FROM Cita c JOIN FETCH c.vehiculo v JOIN FETCH v.cliente "
            + "WHERE c.fechaHora BETWEEN :desde AND :hasta")
    List<Cita> findByFechaHoraBetweenConVehiculoYCliente(LocalDateTime desde, LocalDateTime hasta);
}
