package com.taller.gestion.repository;

import com.taller.gestion.model.CambioEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    List<CambioEstado> findByOrden_IdOrdenOrderByFechaDesc(Long idOrden);
}
