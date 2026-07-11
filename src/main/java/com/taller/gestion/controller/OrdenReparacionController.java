package com.taller.gestion.controller;

import com.taller.gestion.dto.CambioEstadoResponse;
import com.taller.gestion.dto.EstadoRequest;
import com.taller.gestion.dto.OrdenRequest;
import com.taller.gestion.dto.OrdenResponse;
import com.taller.gestion.service.OrdenReparacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenReparacionController {

    private final OrdenReparacionService ordenService;

    public OrdenReparacionController(OrdenReparacionService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crear(req));
    }

    @GetMapping
    public List<OrdenResponse> listar() {
        return ordenService.listar();
    }

    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable Long id) {
        return ordenService.obtener(id);
    }

    @GetMapping("/por-vehiculo/{idVehiculo}")
    public List<OrdenResponse> listarPorVehiculo(@PathVariable Long idVehiculo) {
        return ordenService.listarPorVehiculo(idVehiculo);
    }

    @PutMapping("/{id}")
    public OrdenResponse actualizar(@PathVariable Long id,
                                    @Valid @RequestBody OrdenRequest req) {
        return ordenService.actualizar(id, req);
    }

    // Cambio de estado aislado (lo usara el tablero del dashboard)
    @PutMapping("/{id}/estado")
    public OrdenResponse cambiarEstado(@PathVariable Long id,
                                       @Valid @RequestBody EstadoRequest req) {
        return ordenService.cambiarEstado(id, req.estado());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = ordenService.generarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orden-reparacion-" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/{id}/auditoria")
    public List<CambioEstadoResponse> auditoria(@PathVariable Long id) {
        return ordenService.listarAuditoria(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
