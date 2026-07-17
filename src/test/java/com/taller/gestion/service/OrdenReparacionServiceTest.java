package com.taller.gestion.service;

import com.taller.gestion.dto.LineaRequest;
import com.taller.gestion.dto.OrdenRequest;
import com.taller.gestion.dto.OrdenResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.CambioEstado;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Usuario;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CambioEstadoRepository;
import com.taller.gestion.repository.OrdenReparacionRepository;
import com.taller.gestion.repository.ServicioRepository;
import com.taller.gestion.repository.UsuarioRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Prueba el aggregate de OrdenReparacion + su auditoria de cambios de estado
// (CambioEstado), la parte con mas logica del dominio. Mockito puro: no hace
// falta levantar contexto de Spring ni base de datos para esto.
// El portal publico de seguimiento (obtenerSeguimiento/responderPresupuesto/
// generarPdfPorCodigo) vive en SeguimientoService (ver SeguimientoServiceTest).
@ExtendWith(MockitoExtension.class)
class OrdenReparacionServiceTest {

    @Mock private OrdenReparacionRepository ordenRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private CambioEstadoRepository cambioEstadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PdfOrdenGenerator pdfGenerator;

    private OrdenReparacionService service;

    @BeforeEach
    void prepararAutenticacion() {
        service = new OrdenReparacionService(ordenRepository, vehiculoRepository,
                servicioRepository, cambioEstadoRepository, usuarioRepository, pdfGenerator);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null));
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    private OrdenReparacion ordenConEstado(String estado) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(1L);
        vehiculo.setMatricula("1234ABC");
        vehiculo.setTipo("COCHE");

        OrdenReparacion orden = new OrdenReparacion();
        orden.setIdOrden(1L);
        orden.setVehiculo(vehiculo);
        orden.setEstado(estado);
        return orden;
    }

    @Test
    void cambiarEstadoRegistraLaAuditoriaConElUsuarioAutenticado() {
        OrdenReparacion orden = ordenConEstado("RECEPCION");
        Usuario admin = new Usuario();
        admin.setUsername("admin");

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        service.cambiarEstado(1L, "DIAGNOSTICO");

        assertThat(orden.getEstado()).isEqualTo("DIAGNOSTICO");
        verify(cambioEstadoRepository).save(argThat(cambio ->
                cambio.getEstadoAnterior().equals("RECEPCION")
                        && cambio.getEstadoNuevo().equals("DIAGNOSTICO")
                        && cambio.getUsuario() == admin));
    }

    @Test
    void cambiarEstadoAlMismoValorNoGeneraAuditoria() {
        OrdenReparacion orden = ordenConEstado("EN_REPARACION");

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, "EN_REPARACION");

        verify(cambioEstadoRepository, never()).save(any(CambioEstado.class));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void alEntregarSeRellenaLaFechaDeSalidaAutomaticamente() {
        OrdenReparacion orden = ordenConEstado("LISTO");
        Usuario admin = new Usuario();
        admin.setUsername("admin");

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(orden.getFechaSalida()).isNull();

        service.cambiarEstado(1L, "ENTREGADO");

        assertThat(orden.getFechaSalida()).isNotNull();
    }

    @Test
    void crearGuardaLaOrdenConSusLineasYCalculaElTotal() {
        Vehiculo vehiculo = ordenConEstado("RECEPCION").getVehiculo();
        LineaRequest linea = new LineaRequest(null, "MANO_OBRA", "Cambio de aceite",
                new BigDecimal("1.00"), new BigDecimal("30.00"));
        OrdenRequest req = new OrdenRequest(1L, "Ruido en el motor", 50000, null, List.of(linea));

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(ordenRepository.save(any())).thenAnswer(inv -> {
            OrdenReparacion o = inv.getArgument(0);
            o.setIdOrden(1L);
            return o;
        });

        OrdenResponse resultado = service.crear(req);

        assertThat(resultado.idOrden()).isEqualTo(1L);
        assertThat(resultado.total()).isEqualByComparingTo("30.00");
        assertThat(resultado.lineas()).hasSize(1);
    }

    @Test
    void crearLanzaExcepcionSiElVehiculoNoExiste() {
        OrdenRequest req = new OrdenRequest(99L, "Avería", null, null, null);
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearLanzaExcepcionSiUnaLineaReferenciaUnServicioInexistente() {
        Vehiculo vehiculo = ordenConEstado("RECEPCION").getVehiculo();
        LineaRequest linea = new LineaRequest(99L, "PIEZA", "Filtro", BigDecimal.ONE, new BigDecimal("10.00"));
        OrdenRequest req = new OrdenRequest(1L, "Avería", null, null, List.of(linea));

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerLanzaExcepcionSiLaOrdenNoExiste() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarReemplazaLasLineasExistentes() {
        OrdenReparacion orden = ordenConEstado("RECEPCION");
        Vehiculo vehiculo = orden.getVehiculo();
        LineaRequest nuevaLinea = new LineaRequest(null, "PIEZA", "Filtro de aire", BigDecimal.ONE, new BigDecimal("15.00"));
        OrdenRequest req = new OrdenRequest(1L, "Nueva descripción", 51000, null, List.of(nuevaLinea));

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse resultado = service.actualizar(1L, req);

        assertThat(resultado.descripcionProblema()).isEqualTo("Nueva descripción");
        assertThat(resultado.lineas()).hasSize(1);
        assertThat(resultado.total()).isEqualByComparingTo("15.00");
    }

    @Test
    void eliminarBorraLaOrdenExistente() {
        OrdenReparacion orden = ordenConEstado("RECEPCION");
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        service.eliminar(1L);

        verify(ordenRepository).delete(orden);
    }

    @Test
    void eliminarLanzaExcepcionSiLaOrdenNoExiste() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void generarPdfDelegaEnElGeneradorConLaOrdenBuscada() {
        OrdenReparacion orden = ordenConEstado("RECEPCION");
        byte[] pdfEsperado = {1, 2, 3};

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(pdfGenerator.generar(orden)).thenReturn(pdfEsperado);

        byte[] resultado = service.generarPdf(1L);

        assertThat(resultado).isEqualTo(pdfEsperado);
    }
}
