package com.taller.gestion.service;

import com.taller.gestion.dto.CitaRequest;
import com.taller.gestion.dto.CitaResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cita;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CitaRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock private CitaRepository citaRepository;
    @Mock private VehiculoRepository vehiculoRepository;

    private CitaService service;

    @BeforeEach
    void prepararService() {
        service = new CitaService(citaRepository, vehiculoRepository);
    }

    private Vehiculo vehiculoConId(Long id) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNombre("Cliente Test");

        Vehiculo v = new Vehiculo();
        v.setIdVehiculo(id);
        v.setMatricula("1234ABC");
        v.setTipo("COCHE");
        v.setCliente(cliente);
        return v;
    }

    private Cita citaConId(Long id, Vehiculo vehiculo) {
        Cita c = new Cita();
        c.setIdCita(id);
        c.setVehiculo(vehiculo);
        c.setFechaHora(LocalDateTime.now().plusDays(1));
        c.setEstado("PENDIENTE");
        return c;
    }

    @Test
    void crearGuardaLaCitaConElVehiculoIndicado() {
        Vehiculo vehiculo = vehiculoConId(1L);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1);
        CitaRequest req = new CitaRequest(1L, fecha, "Revisión", null);

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setIdCita(1L);
            return c;
        });

        CitaResponse resultado = service.crear(req);

        assertThat(resultado.idVehiculo()).isEqualTo(1L);
        assertThat(resultado.motivo()).isEqualTo("Revisión");
    }

    @Test
    void crearLanzaExcepcionSiElVehiculoNoExiste() {
        CitaRequest req = new CitaRequest(99L, LocalDateTime.now(), "Revisión", null);
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void convertirEnlazaLaOrdenYMarcaLaCitaComoCompletada() {
        Vehiculo vehiculo = vehiculoConId(1L);
        Cita cita = citaConId(1L, vehiculo);

        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CitaResponse resultado = service.convertir(1L, 42L);

        assertThat(resultado.estado()).isEqualTo("COMPLETADA");
        assertThat(resultado.idOrdenGenerada()).isEqualTo(42L);
    }

    @Test
    void convertirLanzaExcepcionSiLaCitaNoExiste() {
        when(citaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convertir(99L, 1L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarLanzaExcepcionSiLaCitaNoExiste() {
        when(citaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
