package com.taller.gestion.service;

import com.taller.gestion.dto.VehiculoRequest;
import com.taller.gestion.dto.VehiculoResponse;
import com.taller.gestion.exception.ConflictoException;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.ClienteRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ClienteRepository clienteRepository;

    private VehiculoService service;

    @BeforeEach
    void prepararService() {
        service = new VehiculoService(vehiculoRepository, clienteRepository);
    }

    private Cliente clienteConId(Long id) {
        Cliente c = new Cliente();
        c.setIdCliente(id);
        c.setNombre("Cliente Test");
        return c;
    }

    private Vehiculo vehiculoConId(Long id, String matricula, Cliente cliente) {
        Vehiculo v = new Vehiculo();
        v.setIdVehiculo(id);
        v.setMatricula(matricula);
        v.setMarca("Seat");
        v.setModelo("Ibiza");
        v.setTipo("COCHE");
        v.setCliente(cliente);
        return v;
    }

    @Test
    void crearGuardaElVehiculoSiLaMatriculaNoEstaRepetida() {
        Cliente cliente = clienteConId(1L);
        VehiculoRequest req = new VehiculoRequest("1234ABC", "Seat", "Ibiza", (short) 2020, null, 50000, "COCHE", 1L);

        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(vehiculoRepository.save(any())).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            v.setIdVehiculo(1L);
            return v;
        });

        VehiculoResponse resultado = service.crear(req);

        assertThat(resultado.matricula()).isEqualTo("1234ABC");
        assertThat(resultado.idCliente()).isEqualTo(1L);
    }

    @Test
    void crearLanzaConflictoSiLaMatriculaYaExiste() {
        VehiculoRequest req = new VehiculoRequest("1234ABC", "Seat", "Ibiza", (short) 2020, null, 50000, "COCHE", 1L);
        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void crearLanzaExcepcionSiElClienteNoExiste() {
        VehiculoRequest req = new VehiculoRequest("1234ABC", "Seat", "Ibiza", (short) 2020, null, 50000, "COCHE", 99L);
        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(false);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarPermiteMantenerLaMismaMatricula() {
        Cliente cliente = clienteConId(1L);
        Vehiculo existente = vehiculoConId(1L, "1234ABC", cliente);
        VehiculoRequest req = new VehiculoRequest("1234ABC", "Seat", "León", (short) 2021, null, 60000, "COCHE", 1L);

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(vehiculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponse resultado = service.actualizar(1L, req);

        assertThat(resultado.modelo()).isEqualTo("León");
    }

    @Test
    void actualizarLanzaConflictoSiLaNuevaMatriculaYaPerteneceAOtroVehiculo() {
        Cliente cliente = clienteConId(1L);
        Vehiculo existente = vehiculoConId(1L, "1234ABC", cliente);
        VehiculoRequest req = new VehiculoRequest("9999ZZZ", "Seat", "León", (short) 2021, null, 60000, "COCHE", 1L);

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(vehiculoRepository.existsByMatricula("9999ZZZ")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, req)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarLanzaExcepcionSiElVehiculoNoExiste() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarPorClienteLanzaExcepcionSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorCliente(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
