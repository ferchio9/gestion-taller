package com.taller.gestion.service;

import com.taller.gestion.dto.ClienteRequest;
import com.taller.gestion.dto.ClienteResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;

    private ClienteService service;

    @BeforeEach
    void prepararService() {
        service = new ClienteService(clienteRepository);
    }

    private Cliente clienteConId(Long id) {
        Cliente c = new Cliente();
        c.setIdCliente(id);
        c.setNombre("Juan Pérez");
        return c;
    }

    @Test
    void crearGuardaElClienteConLosDatosDeLaPeticion() {
        ClienteRequest req = new ClienteRequest("Juan Pérez", "600111222", "juan@test.com", "12345678A");
        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setIdCliente(1L);
            return c;
        });

        ClienteResponse resultado = service.crear(req);

        assertThat(resultado.idCliente()).isEqualTo(1L);
        assertThat(resultado.nombre()).isEqualTo("Juan Pérez");
        assertThat(resultado.email()).isEqualTo("juan@test.com");
    }

    @Test
    void obtenerLanzaExcepcionSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarModificaLosCamposDelClienteExistente() {
        Cliente existente = clienteConId(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteRequest req = new ClienteRequest("Juan Actualizado", "600999888", "nuevo@test.com", "87654321B");
        ClienteResponse resultado = service.actualizar(1L, req);

        assertThat(resultado.nombre()).isEqualTo("Juan Actualizado");
        assertThat(resultado.telefono()).isEqualTo("600999888");
    }

    @Test
    void actualizarLanzaExcepcionSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        ClienteRequest req = new ClienteRequest("Nombre", null, null, null);

        assertThatThrownBy(() -> service.actualizar(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarBorraElClienteExistente() {
        Cliente existente = clienteConId(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(clienteRepository).delete(existente);
    }

    @Test
    void eliminarLanzaExcepcionSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
