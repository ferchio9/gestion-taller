package com.taller.gestion.dto;

import java.math.BigDecimal;

public record LineaResponse(
        Long idLinea,
        Long idServicio,
        String tipo,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal importe   // calculado: cantidad * precioUnitario
) {
}
