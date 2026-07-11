package com.taller.gestion.repository;

import com.taller.gestion.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Buscar por matricula (unica). Optional = "puede no existir".
    Optional<Vehiculo> findByMatricula(String matricula);

    // Para comprobar duplicados sin traerte la fila entera.
    boolean existsByMatricula(String matricula);

    // Vehiculos de un cliente. El guion bajo marca el salto de propiedad:
    // Vehiculo -> cliente -> idCliente.
    List<Vehiculo> findByCliente_IdCliente(Long idCliente);
}
