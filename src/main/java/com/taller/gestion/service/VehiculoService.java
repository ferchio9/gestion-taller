package com.taller.gestion.service;

import com.taller.gestion.dto.VehiculoRequest;
import com.taller.gestion.dto.VehiculoResponse;
import com.taller.gestion.exception.ConflictoException;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.Vehiculo;
import com.taller.gestion.repository.ClienteRepository;
import com.taller.gestion.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository,
                           ClienteRepository clienteRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest req) {
        if (vehiculoRepository.existsByMatricula(req.matricula())) {
            throw new ConflictoException(
                    "Ya hay un vehículo registrado con la matrícula " + req.matricula());
        }
        Cliente cliente = buscarCliente(req.idCliente());

        Vehiculo v = new Vehiculo();
        aplicar(v, req);
        v.setCliente(cliente);
        return toResponse(vehiculoRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar() {
        return vehiculoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPorCliente(Long idCliente) {
        buscarCliente(idCliente); // 404 claro si el cliente no existe
        return vehiculoRepository.findByCliente_IdCliente(idCliente)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest req) {
        Vehiculo v = buscar(id);

        // Si cambia la matricula, comprobar que la nueva no choque con otra.
        if (!v.getMatricula().equals(req.matricula())
                && vehiculoRepository.existsByMatricula(req.matricula())) {
            throw new ConflictoException(
                    "Ya hay un vehículo registrado con la matrícula " + req.matricula());
        }
        aplicar(v, req);
        v.setCliente(buscarCliente(req.idCliente()));
        return toResponse(vehiculoRepository.save(v));
    }

    @Transactional
    public void eliminar(Long id) {
        vehiculoRepository.delete(buscar(id));
    }

    // ---------- helpers ----------

    private Vehiculo buscar(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún vehículo con el identificador " + id));
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún cliente con el identificador " + idCliente));
    }

    private void aplicar(Vehiculo v, VehiculoRequest req) {
        v.setMatricula(req.matricula());
        v.setMarca(req.marca());
        v.setModelo(req.modelo());
        v.setAnio(req.anio());
        v.setBastidorVin(req.bastidorVin());
        v.setKmActual(req.kmActual());
        v.setTipo(req.tipo());
    }

    private VehiculoResponse toResponse(Vehiculo v) {
        return new VehiculoResponse(
                v.getIdVehiculo(), v.getMatricula(), v.getMarca(), v.getModelo(),
                v.getAnio(), v.getBastidorVin(), v.getKmActual(), v.getTipo(),
                v.getCliente().getIdCliente(), v.getCliente().getNombre());
    }
}
