package com.taller.gestion.service;

import com.taller.gestion.dto.SeguimientoResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CambioEstadoRepository;
import com.taller.gestion.repository.OrdenReparacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock private OrdenReparacionRepository ordenRepository;
    @Mock private CambioEstadoRepository cambioEstadoRepository;
    @Mock private PdfOrdenGenerator pdfGenerator;

    private SeguimientoService service;

    @BeforeEach
    void prepararService() {
        service = new SeguimientoService(ordenRepository, cambioEstadoRepository, pdfGenerator);
    }

    private OrdenReparacion ordenConCodigo(String codigo) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNombre("Cliente Test");

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(1L);
        vehiculo.setMatricula("1234ABC");
        vehiculo.setMarca("Seat");
        vehiculo.setModelo("Ibiza");
        vehiculo.setTipo("COCHE");
        vehiculo.setCliente(cliente);

        OrdenReparacion orden = new OrdenReparacion();
        orden.setIdOrden(1L);
        orden.setVehiculo(vehiculo);
        orden.setEstado("EN_REPARACION");
        orden.setCodigoSeguimiento(codigo);
        return orden;
    }

    @Test
    void obtenerSeguimientoDevuelveLosDatosPublicosDeLaOrden() {
        OrdenReparacion orden = ordenConCodigo("codigo-123");

        when(ordenRepository.findByCodigoSeguimiento("codigo-123")).thenReturn(Optional.of(orden));
        when(cambioEstadoRepository.findByOrden_IdOrdenOrderByFechaDesc(1L)).thenReturn(List.of());

        SeguimientoResponse resultado = service.obtenerSeguimiento("codigo-123");

        assertThat(resultado.matricula()).isEqualTo("1234ABC");
        assertThat(resultado.nombreCliente()).isEqualTo("Cliente Test");
        assertThat(resultado.estado()).isEqualTo("EN_REPARACION");
    }

    @Test
    void obtenerSeguimientoLanzaExcepcionSiElCodigoNoExiste() {
        when(ordenRepository.findByCodigoSeguimiento("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSeguimiento("inexistente"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void responderPresupuestoActualizaLaAprobacionYLaFecha() {
        OrdenReparacion orden = ordenConCodigo("codigo-123");
        when(ordenRepository.findByCodigoSeguimiento("codigo-123")).thenReturn(Optional.of(orden));

        service.responderPresupuesto("codigo-123", true);

        assertThat(orden.getPresupuestoAprobado()).isTrue();
        assertThat(orden.getPresupuestoRespondidoEn()).isNotNull();
        verify(ordenRepository).save(orden);
    }

    @Test
    void responderPresupuestoLanzaExcepcionSiElCodigoNoExiste() {
        when(ordenRepository.findByCodigoSeguimiento("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.responderPresupuesto("inexistente", true))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void generarPdfPorCodigoDelegaEnElGeneradorConLaOrdenBuscada() {
        OrdenReparacion orden = ordenConCodigo("codigo-123");
        byte[] pdfEsperado = {1, 2, 3};

        when(ordenRepository.findByCodigoSeguimiento("codigo-123")).thenReturn(Optional.of(orden));
        when(pdfGenerator.generar(orden)).thenReturn(pdfEsperado);

        byte[] resultado = service.generarPdfPorCodigo("codigo-123");

        assertThat(resultado).isEqualTo(pdfEsperado);
    }

    @Test
    void generarPdfPorCodigoLanzaExcepcionSiElCodigoNoExiste() {
        when(ordenRepository.findByCodigoSeguimiento("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarPdfPorCodigo("inexistente"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
