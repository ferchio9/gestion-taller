package com.taller.gestion.service;

import com.taller.gestion.dto.*;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.CambioEstado;
import com.taller.gestion.model.LineaOrden;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Servicio;
import com.taller.gestion.model.Usuario;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CambioEstadoRepository;
import com.taller.gestion.repository.OrdenReparacionRepository;
import com.taller.gestion.repository.ServicioRepository;
import com.taller.gestion.repository.UsuarioRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdenReparacionService {

    private static final Logger log = LoggerFactory.getLogger(OrdenReparacionService.class);

    private final OrdenReparacionRepository ordenRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ServicioRepository servicioRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PdfOrdenGenerator pdfGenerator;

    public OrdenReparacionService(OrdenReparacionRepository ordenRepository,
                                  VehiculoRepository vehiculoRepository,
                                  ServicioRepository servicioRepository,
                                  CambioEstadoRepository cambioEstadoRepository,
                                  UsuarioRepository usuarioRepository,
                                  PdfOrdenGenerator pdfGenerator) {
        this.ordenRepository = ordenRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.servicioRepository = servicioRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional
    public OrdenResponse crear(OrdenRequest req) {
        Vehiculo vehiculo = buscarVehiculo(req.idVehiculo());

        OrdenReparacion orden = new OrdenReparacion();
        orden.setVehiculo(vehiculo);
        orden.setDescripcionProblema(req.descripcionProblema());
        orden.setKmEntrada(req.kmEntrada());
        if (req.estado() != null) {
            orden.setEstado(req.estado());
        }
        anadirLineas(orden, req.lineas());

        // Al guardar la orden se guardan sus lineas en cascada.
        OrdenReparacion guardada = ordenRepository.save(orden);
        log.info("Orden de reparación creada: id={}, idVehiculo={}", guardada.getIdOrden(), vehiculo.getIdVehiculo());
        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar() {
        return ordenRepository.findAllConVehiculoYLineas().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listarPorVehiculo(Long idVehiculo) {
        buscarVehiculo(idVehiculo); // 404 claro si no existe
        return ordenRepository.findByVehiculo_IdVehiculo(idVehiculo)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrdenResponse actualizar(Long id, OrdenRequest req) {
        OrdenReparacion orden = buscar(id);
        orden.setVehiculo(buscarVehiculo(req.idVehiculo()));
        orden.setDescripcionProblema(req.descripcionProblema());
        orden.setKmEntrada(req.kmEntrada());
        if (req.estado() != null) {
            orden.setEstado(req.estado());
        }

        // Reemplazo del conjunto de lineas: vaciar (orphanRemoval las borra) y volver a anadir.
        orden.getLineas().clear();
        anadirLineas(orden, req.lineas());

        OrdenResponse resultado = toResponse(ordenRepository.save(orden));
        log.info("Orden de reparación actualizada: id={}", id);
        return resultado;
    }

    @Transactional
    public OrdenResponse cambiarEstado(Long id, String nuevoEstado) {
        OrdenReparacion orden = buscar(id);
        String estadoAnterior = orden.getEstado();
        orden.setEstado(nuevoEstado);
        // Al entregar, registramos la fecha de salida automaticamente.
        if ("ENTREGADO".equals(nuevoEstado) && orden.getFechaSalida() == null) {
            orden.setFechaSalida(LocalDateTime.now());
        }
        OrdenResponse resultado = toResponse(ordenRepository.save(orden));
        registrarAuditoria(orden, estadoAnterior, nuevoEstado);
        log.info("Cambio de estado de orden: id={}, {} -> {}", id, estadoAnterior, nuevoEstado);
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<CambioEstadoResponse> listarAuditoria(Long id) {
        buscar(id); // 404 claro si no existe
        return cambioEstadoRepository.findByOrden_IdOrdenOrderByFechaDesc(id).stream()
                .map(c -> new CambioEstadoResponse(
                        c.getFecha(), c.getEstadoAnterior(), c.getEstadoNuevo(), c.getUsuario().getUsername()))
                .toList();
    }

    // No audita si el estado no cambia realmente (p. ej. mismo valor reenviado).
    private void registrarAuditoria(OrdenReparacion orden, String estadoAnterior, String estadoNuevo) {
        if (estadoAnterior != null && estadoAnterior.equals(estadoNuevo)) {
            return;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + username));

        CambioEstado cambio = new CambioEstado();
        cambio.setOrden(orden);
        cambio.setEstadoAnterior(estadoAnterior);
        cambio.setEstadoNuevo(estadoNuevo);
        cambio.setUsuario(usuario);
        cambioEstadoRepository.save(cambio);
    }

    @Transactional
    public void eliminar(Long id) {
        // Al borrar la orden, sus lineas se borran en cascada.
        ordenRepository.delete(buscar(id));
        log.info("Orden de reparación eliminada: id={}", id);
    }

    // readOnly: solo lee. Se mantiene la transaccion abierta mientras el generador
    // recorre orden -> vehiculo -> cliente y orden -> lineas (relaciones LAZY).
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id) {
        OrdenReparacion orden = buscar(id);
        return pdfGenerator.generar(orden);
    }

    // ---------- helpers ----------

    private void anadirLineas(OrdenReparacion orden, List<LineaRequest> lineas) {
        if (lineas == null) {
            return;
        }
        for (LineaRequest lr : lineas) {
            LineaOrden linea = new LineaOrden();
            linea.setTipo(lr.tipo());
            linea.setDescripcion(lr.descripcion());
            linea.setCantidad(lr.cantidad());
            linea.setPrecioUnitario(lr.precioUnitario());
            if (lr.idServicio() != null) {
                linea.setServicio(buscarServicio(lr.idServicio()));
            }
            orden.addLinea(linea);
        }
    }

    private OrdenReparacion buscar(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ninguna orden de reparación con el identificador " + id));
    }

    private Vehiculo buscarVehiculo(Long idVehiculo) {
        return vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún vehículo con el identificador " + idVehiculo));
    }

    private Servicio buscarServicio(Long idServicio) {
        return servicioRepository.findById(idServicio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún servicio con el identificador " + idServicio));
    }

    private OrdenResponse toResponse(OrdenReparacion orden) {
        List<LineaResponse> lineas = orden.getLineas().stream()
                .map(this::toLineaResponse)
                .toList();

        BigDecimal total = lineas.stream()
                .map(LineaResponse::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrdenResponse(
                orden.getIdOrden(),
                orden.getVehiculo().getIdVehiculo(),
                orden.getVehiculo().getMatricula(),
                orden.getVehiculo().getTipo(),
                orden.getEstado(),
                orden.getFechaEntrada(),
                orden.getFechaSalida(),
                orden.getDescripcionProblema(),
                orden.getKmEntrada(),
                total,
                lineas,
                orden.getCodigoSeguimiento(),
                orden.getPresupuestoAprobado());
    }

    private LineaResponse toLineaResponse(LineaOrden l) {
        BigDecimal importe = l.getCantidad().multiply(l.getPrecioUnitario());
        Long idServicio = (l.getServicio() != null) ? l.getServicio().getIdServicio() : null;
        return new LineaResponse(
                l.getIdLinea(), idServicio, l.getTipo(), l.getDescripcion(),
                l.getCantidad(), l.getPrecioUnitario(), importe);
    }
}
