package com.taller.gestion.service;

import com.taller.gestion.dto.ServicioRequest;
import com.taller.gestion.dto.ServicioResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Servicio;
import com.taller.gestion.repository.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock private ServicioRepository servicioRepository;

    private ServicioService service;

    @BeforeEach
    void prepararService() {
        service = new ServicioService(servicioRepository);
    }

    private Servicio servicioConId(Long id) {
        Servicio s = new Servicio();
        s.setIdServicio(id);
        s.setNombre("Cambio de aceite");
        s.setTipo("MANO_OBRA");
        s.setPrecioBase(new BigDecimal("30.00"));
        return s;
    }

    @Test
    void crearGuardaElServicioConLosDatosDeLaPeticion() {
        ServicioRequest req = new ServicioRequest("Cambio de aceite", "MANO_OBRA", new BigDecimal("30.00"));
        when(servicioRepository.save(any())).thenAnswer(inv -> {
            Servicio s = inv.getArgument(0);
            s.setIdServicio(1L);
            return s;
        });

        ServicioResponse resultado = service.crear(req);

        assertThat(resultado.idServicio()).isEqualTo(1L);
        assertThat(resultado.precioBase()).isEqualByComparingTo("30.00");
    }

    @Test
    void obtenerLanzaExcepcionSiElServicioNoExiste() {
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarModificaElPrecioDelServicioExistente() {
        Servicio existente = servicioConId(1L);
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(servicioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServicioRequest req = new ServicioRequest("Cambio de aceite", "MANO_OBRA", new BigDecimal("35.50"));
        ServicioResponse resultado = service.actualizar(1L, req);

        assertThat(resultado.precioBase()).isEqualByComparingTo("35.50");
    }

    @Test
    void eliminarBorraElServicioExistente() {
        Servicio existente = servicioConId(1L);
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(servicioRepository).delete(existente);
    }

    @Test
    void eliminarLanzaExcepcionSiElServicioNoExiste() {
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
