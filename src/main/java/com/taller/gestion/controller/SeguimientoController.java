package com.taller.gestion.controller;

import com.taller.gestion.dto.AprobacionRequest;
import com.taller.gestion.dto.SeguimientoResponse;
import com.taller.gestion.service.SeguimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Publico (permitAll en SecurityConfig): el codigo de seguimiento en la URL hace
// de credencial. No expone datos sensibles del cliente (ver SeguimientoResponse).
@RestController
@RequestMapping("/api/seguimiento")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping("/{codigo}")
    public SeguimientoResponse obtener(@PathVariable String codigo) {
        return seguimientoService.obtenerSeguimiento(codigo);
    }

    @PutMapping("/{codigo}/presupuesto")
    public SeguimientoResponse responderPresupuesto(@PathVariable String codigo,
                                                      @Valid @RequestBody AprobacionRequest req) {
        seguimientoService.responderPresupuesto(codigo, req.aprobado());
        return seguimientoService.obtenerSeguimiento(codigo);
    }

    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String codigo) {
        byte[] pdf = seguimientoService.generarPdfPorCodigo(codigo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orden-reparacion.pdf\"")
                .body(pdf);
    }
}
