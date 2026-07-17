package com.taller.gestion.service;

import com.taller.gestion.dto.CambioEstadoResponse;
import com.taller.gestion.dto.LineaResponse;
import com.taller.gestion.dto.SeguimientoResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.LineaOrden;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CambioEstadoRepository;
import com.taller.gestion.repository.OrdenReparacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Portal de seguimiento publico (sin login): el codigo UUID de la orden hace de
// credencial. Separado de OrdenReparacionService (gestion interna) porque atiende
// a un cliente distinto (el propietario del vehiculo, sin autenticar) con sus
// propias reglas de acceso (ver SecurityConfig: /api/seguimiento/** es permitAll).
@Service
public class SeguimientoService {

    private static final Logger log = LoggerFactory.getLogger(SeguimientoService.class);

    private final OrdenReparacionRepository ordenRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final PdfOrdenGenerator pdfGenerator;

    public SeguimientoService(OrdenReparacionRepository ordenRepository,
                              CambioEstadoRepository cambioEstadoRepository,
                              PdfOrdenGenerator pdfGenerator) {
        this.ordenRepository = ordenRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public SeguimientoResponse obtenerSeguimiento(String codigo) {
        OrdenReparacion orden = buscarPorCodigo(codigo);
        Vehiculo vehiculo = orden.getVehiculo();

        List<LineaResponse> lineas = orden.getLineas().stream().map(this::toLineaResponse).toList();
        BigDecimal total = lineas.stream().map(LineaResponse::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CambioEstadoResponse> historial = cambioEstadoRepository
                .findByOrden_IdOrdenOrderByFechaDesc(orden.getIdOrden()).stream()
                .map(c -> new CambioEstadoResponse(c.getFecha(), c.getEstadoAnterior(), c.getEstadoNuevo(), c.getUsuario().getUsername()))
                .toList();

        return new SeguimientoResponse(
                vehiculo.getMatricula(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getTipo(),
                vehiculo.getCliente().getNombre(),
                orden.getEstado(),
                orden.getFechaEntrada(),
                orden.getFechaSalida(),
                orden.getDescripcionProblema(),
                total,
                lineas,
                orden.getPresupuestoAprobado(),
                orden.getPresupuestoRespondidoEn(),
                historial);
    }

    @Transactional
    public void responderPresupuesto(String codigo, boolean aprobado) {
        OrdenReparacion orden = buscarPorCodigo(codigo);
        orden.setPresupuestoAprobado(aprobado);
        orden.setPresupuestoRespondidoEn(LocalDateTime.now());
        ordenRepository.save(orden);
        log.info("Respuesta de presupuesto: codigo={}, aprobado={}", codigo, aprobado);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfPorCodigo(String codigo) {
        return pdfGenerator.generar(buscarPorCodigo(codigo));
    }

    private OrdenReparacion buscarPorCodigo(String codigo) {
        return ordenRepository.findByCodigoSeguimiento(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ninguna orden de reparación con ese código de seguimiento"));
    }

    private LineaResponse toLineaResponse(LineaOrden l) {
        BigDecimal importe = l.getCantidad().multiply(l.getPrecioUnitario());
        Long idServicio = (l.getServicio() != null) ? l.getServicio().getIdServicio() : null;
        return new LineaResponse(
                l.getIdLinea(), idServicio, l.getTipo(), l.getDescripcion(),
                l.getCantidad(), l.getPrecioUnitario(), importe);
    }
}
