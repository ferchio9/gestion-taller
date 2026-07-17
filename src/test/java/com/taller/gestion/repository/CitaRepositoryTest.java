package com.taller.gestion.repository;

import com.taller.gestion.model.Cita;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.Vehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CitaRepository citaRepository;

    private Vehiculo crearVehiculoConCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Test");
        entityManager.persist(cliente);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMatricula("1234ABC");
        vehiculo.setMarca("Seat");
        vehiculo.setModelo("Ibiza");
        vehiculo.setTipo("COCHE");
        vehiculo.setCliente(cliente);
        entityManager.persist(vehiculo);
        return vehiculo;
    }

    @Test
    void findByFechaHoraBetweenConVehiculoYClienteSoloDevuelveCitasDentroDelRango() {
        Vehiculo vehiculo = crearVehiculoConCliente();

        Cita dentroDelRango = new Cita();
        dentroDelRango.setVehiculo(vehiculo);
        dentroDelRango.setFechaHora(LocalDateTime.of(2026, 6, 15, 10, 0));
        entityManager.persist(dentroDelRango);

        Cita fueraDelRango = new Cita();
        fueraDelRango.setVehiculo(vehiculo);
        fueraDelRango.setFechaHora(LocalDateTime.of(2026, 1, 1, 10, 0));
        entityManager.persistAndFlush(fueraDelRango);
        entityManager.clear();

        List<Cita> resultado = citaRepository.findByFechaHoraBetweenConVehiculoYCliente(
                LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 23, 59));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculo().getCliente().getNombre()).isEqualTo("Cliente Test");
    }

    @Test
    void findAllConVehiculoYClienteDevuelveTodasLasCitas() {
        Vehiculo vehiculo = crearVehiculoConCliente();
        Cita cita = new Cita();
        cita.setVehiculo(vehiculo);
        cita.setFechaHora(LocalDateTime.now());
        entityManager.persistAndFlush(cita);
        entityManager.clear();

        List<Cita> resultado = citaRepository.findAllConVehiculoYCliente();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculo().getMatricula()).isEqualTo("1234ABC");
    }
}
