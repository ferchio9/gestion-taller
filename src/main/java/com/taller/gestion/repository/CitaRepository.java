package com.taller.gestion.repository;

import com.taller.gestion.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByVehiculo_IdVehiculo(Long idVehiculo);
}
