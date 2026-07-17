package com.taller.gestion.service;

import com.taller.gestion.dto.CitaRequest;
import com.taller.gestion.dto.CitaResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cita;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.CitaRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    private final CitaRepository citaRepository;
    private final VehiculoRepository vehiculoRepository;

    public CitaService(CitaRepository citaRepository, VehiculoRepository vehiculoRepository) {
        this.citaRepository = citaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional
    public CitaResponse crear(CitaRequest req) {
        Cita cita = new Cita();
        cita.setVehiculo(buscarVehiculo(req.idVehiculo()));
        cita.setFechaHora(req.fechaHora());
        cita.setMotivo(req.motivo());
        if (req.estado() != null) {
            cita.setEstado(req.estado());
        }
        Cita guardada = citaRepository.save(cita);
        log.info("Cita creada: id={}", guardada.getIdCita());
        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listar(LocalDateTime desde, LocalDateTime hasta) {
        List<Cita> citas = (desde != null && hasta != null)
                ? citaRepository.findByFechaHoraBetweenConVehiculoYCliente(desde, hasta)
                : citaRepository.findAllConVehiculoYCliente();
        return citas.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest req) {
        Cita cita = buscar(id);
        cita.setVehiculo(buscarVehiculo(req.idVehiculo()));
        cita.setFechaHora(req.fechaHora());
        cita.setMotivo(req.motivo());
        if (req.estado() != null) {
            cita.setEstado(req.estado());
        }
        CitaResponse resultado = toResponse(citaRepository.save(cita));
        log.info("Cita actualizada: id={}", id);
        return resultado;
    }

    @Transactional
    public void eliminar(Long id) {
        citaRepository.delete(buscar(id));
        log.info("Cita eliminada: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorVehiculo(Long idVehiculo) {
        buscarVehiculo(idVehiculo); // 404 claro si no existe
        return citaRepository.findByVehiculo_IdVehiculo(idVehiculo).stream().map(this::toResponse).toList();
    }

    // Enlaza la cita con la orden ya creada a partir de ella y la marca como
    // completada: la conversion es un hecho consumado, no tiene sentido volver
    // a convertir la misma cita una segunda vez.
    @Transactional
    public CitaResponse convertir(Long id, Long idOrden) {
        Cita cita = buscar(id);
        cita.setIdOrdenGenerada(idOrden);
        cita.setEstado("COMPLETADA");
        CitaResponse resultado = toResponse(citaRepository.save(cita));
        log.info("Cita convertida en orden: idCita={}, idOrden={}", id, idOrden);
        return resultado;
    }

    private Cita buscar(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ninguna cita con el identificador " + id));
    }

    private Vehiculo buscarVehiculo(Long idVehiculo) {
        return vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún vehículo con el identificador " + idVehiculo));
    }

    private CitaResponse toResponse(Cita c) {
        Vehiculo v = c.getVehiculo();
        return new CitaResponse(
                c.getIdCita(), v.getIdVehiculo(), v.getMatricula(), v.getTipo(),
                v.getCliente().getNombre(), c.getFechaHora(), c.getMotivo(), c.getEstado(),
                c.getIdOrdenGenerada());
    }
}
