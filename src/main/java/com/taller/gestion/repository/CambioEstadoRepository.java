package com.taller.gestion.repository;

import com.taller.gestion.model.CambioEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    // JOIN FETCH evita el N+1 de listarAuditoria()/obtenerSeguimiento(): ambos
    // navegan cambio -> usuario para leer el username.
    @Query("SELECT c FROM CambioEstado c JOIN FETCH c.usuario WHERE c.orden.idOrden = :idOrden "
            + "ORDER BY c.fecha DESC")
    List<CambioEstado> findByOrden_IdOrdenOrderByFechaDesc(Long idOrden);
}
