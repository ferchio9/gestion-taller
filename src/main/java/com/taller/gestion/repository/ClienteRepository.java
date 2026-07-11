package com.taller.gestion.repository;

import com.taller.gestion.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

// Heredando de JpaRepository ya tienes save, findAll, findById, deleteById, etc.
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
