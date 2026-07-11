package com.taller.gestion.controller;

import com.taller.gestion.dto.VehiculoRequest;
import com.taller.gestion.dto.VehiculoResponse;
import com.taller.gestion.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.crear(req));
    }

    @GetMapping
    public List<VehiculoResponse> listar() {
        return vehiculoService.listar();
    }

    @GetMapping("/{id}")
    public VehiculoResponse obtener(@PathVariable Long id) {
        return vehiculoService.obtener(id);
    }

    @GetMapping("/por-cliente/{idCliente}")
    public List<VehiculoResponse> listarPorCliente(@PathVariable Long idCliente) {
        return vehiculoService.listarPorCliente(idCliente);
    }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody VehiculoRequest req) {
        return vehiculoService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
