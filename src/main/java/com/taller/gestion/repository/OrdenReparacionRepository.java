package com.taller.gestion.repository;

import com.taller.gestion.model.OrdenReparacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// OJO: no hay LineaOrdenRepository a proposito. Las lineas se gestionan
// SIEMPRE a traves de su orden (patron agregado).
public interface OrdenReparacionRepository extends JpaRepository<OrdenReparacion, Long> {

    List<OrdenReparacion> findByVehiculo_IdVehiculo(Long idVehiculo);

    Optional<OrdenReparacion> findByCodigoSeguimiento(String codigoSeguimiento);
}
