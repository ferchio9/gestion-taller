package com.taller.gestion.controller;

import com.taller.gestion.dto.CitaConvertirRequest;
import com.taller.gestion.dto.CitaRequest;
import com.taller.gestion.dto.CitaResponse;
import com.taller.gestion.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(req));
    }

    // Sin desde/hasta devuelve todas las citas (uso puntual); citas.html siempre
    // pasa el rango de la semana visible.
    @GetMapping
    public List<CitaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return citaService.listar(desde, hasta);
    }

    @GetMapping("/por-vehiculo/{idVehiculo}")
    public List<CitaResponse> listarPorVehiculo(@PathVariable Long idVehiculo) {
        return citaService.listarPorVehiculo(idVehiculo);
    }

    @PutMapping("/{id}")
    public CitaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest req) {
        return citaService.actualizar(id, req);
    }

    // Se llama desde crear.js justo despues de guardar la orden nueva que nace
    // de "Convertir en orden de reparación".
    @PutMapping("/{id}/convertir")
    public CitaResponse convertir(@PathVariable Long id, @Valid @RequestBody CitaConvertirRequest req) {
        return citaService.convertir(id, req.idOrden());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        citaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
