package com.taller.gestion.controller;

import com.taller.gestion.dto.AprobacionRequest;
import com.taller.gestion.dto.SeguimientoResponse;
import com.taller.gestion.service.OrdenReparacionService;
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

    private final OrdenReparacionService ordenService;

    public SeguimientoController(OrdenReparacionService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping("/{codigo}")
    public SeguimientoResponse obtener(@PathVariable String codigo) {
        return ordenService.obtenerSeguimiento(codigo);
    }

    @PutMapping("/{codigo}/presupuesto")
    public SeguimientoResponse responderPresupuesto(@PathVariable String codigo,
                                                      @Valid @RequestBody AprobacionRequest req) {
        ordenService.responderPresupuesto(codigo, req.aprobado());
        return ordenService.obtenerSeguimiento(codigo);
    }

    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String codigo) {
        byte[] pdf = ordenService.generarPdfPorCodigo(codigo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orden-reparacion.pdf\"")
                .body(pdf);
    }
}
