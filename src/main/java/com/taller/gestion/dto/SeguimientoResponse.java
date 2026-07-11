package com.taller.gestion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Version publica de una orden, para el portal de seguimiento sin login.
// A proposito NO incluye telefono/email del cliente: solo su nombre.
public record SeguimientoResponse(
        String matricula,
        String marca,
        String modelo,
        String tipo,
        String nombreCliente,
        String estado,
        LocalDateTime fechaEntrada,
        LocalDateTime fechaSalida,
        String descripcionProblema,
        BigDecimal total,
        List<LineaResponse> lineas,
        Boolean presupuestoAprobado,
        LocalDateTime presupuestoRespondidoEn,
        List<CambioEstadoResponse> historial
) {
}
