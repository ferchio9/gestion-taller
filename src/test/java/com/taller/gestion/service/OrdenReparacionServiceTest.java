package com.taller.gestion.service;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Prueba el aggregate de OrdenReparacion + su auditoria de cambios de estado
// (CambioEstado), la parte con mas logica del dominio. Mockito puro: no hace
// falta levantar contexto de Spring ni base de datos para esto.
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
}
