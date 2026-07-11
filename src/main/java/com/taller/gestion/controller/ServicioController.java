package com.taller.gestion.controller;

import com.taller.gestion.dto.ServicioRequest;
import com.taller.gestion.dto.ServicioResponse;
import com.taller.gestion.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(req));
    }

    @GetMapping
    public List<ServicioResponse> listar() {
        return servicioService.listar();
    }

    @GetMapping("/{id}")
    public ServicioResponse obtener(@PathVariable Long id) {
        return servicioService.obtener(id);
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ServicioRequest req) {
        return servicioService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
