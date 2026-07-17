package com.taller.gestion.repository;

import com.taller.gestion.model.OrdenReparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// OJO: no hay LineaOrdenRepository a proposito. Las lineas se gestionan
// SIEMPRE a traves de su orden (patron agregado).
public interface OrdenReparacionRepository extends JpaRepository<OrdenReparacion, Long> {

    List<OrdenReparacion> findByVehiculo_IdVehiculo(Long idVehiculo);

    Optional<OrdenReparacion> findByCodigoSeguimiento(String codigoSeguimiento);

    // JOIN FETCH evita el N+1 de listar(): toResponse() navega orden -> vehiculo y orden -> lineas.
    // DISTINCT evita filas duplicadas por el fetch de la coleccion "lineas".
    @Query("SELECT DISTINCT o FROM OrdenReparacion o JOIN FETCH o.vehiculo LEFT JOIN FETCH o.lineas")
    List<OrdenReparacion> findAllConVehiculoYLineas();
}
