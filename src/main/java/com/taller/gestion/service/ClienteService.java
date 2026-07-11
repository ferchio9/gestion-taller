package com.taller.gestion.service;

import com.taller.gestion.dto.ClienteRequest;
import com.taller.gestion.dto.ClienteResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    // Inyeccion por constructor (mejor practica que @Autowired en campo).
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest req) {
        Cliente cliente = new Cliente();
        cliente.setNombre(req.nombre());
        cliente.setTelefono(req.telefono());
        cliente.setEmail(req.email());
        cliente.setNifCif(req.nifCif());
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest req) {
        Cliente cliente = buscar(id);
        cliente.setNombre(req.nombre());
        cliente.setTelefono(req.telefono());
        cliente.setEmail(req.email());
        cliente.setNifCif(req.nifCif());
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        clienteRepository.delete(buscar(id));
    }

    // ---------- helpers ----------

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún cliente con el identificador " + id));
    }

    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(
                c.getIdCliente(), c.getNombre(), c.getTelefono(),
                c.getEmail(), c.getNifCif(), c.getFechaAlta());
    }
}
