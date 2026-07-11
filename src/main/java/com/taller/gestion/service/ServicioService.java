package com.taller.gestion.service;

import com.taller.gestion.dto.ServicioRequest;
import com.taller.gestion.dto.ServicioResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Servicio;
import com.taller.gestion.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest req) {
        Servicio s = new Servicio();
        s.setNombre(req.nombre());
        s.setTipo(req.tipo());
        s.setPrecioBase(req.precioBase());
        return toResponse(servicioRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar() {
        return servicioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest req) {
        Servicio s = buscar(id);
        s.setNombre(req.nombre());
        s.setTipo(req.tipo());
        s.setPrecioBase(req.precioBase());
        return toResponse(servicioRepository.save(s));
    }

    @Transactional
    public void eliminar(Long id) {
        servicioRepository.delete(buscar(id));
    }

    private Servicio buscar(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el servicio con id " + id));
    }

    private ServicioResponse toResponse(Servicio s) {
        return new ServicioResponse(
                s.getIdServicio(), s.getNombre(), s.getTipo(), s.getPrecioBase());
    }
}
