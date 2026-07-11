package com.taller.gestion.dto;

import java.math.BigDecimal;

public record ServicioResponse(
        Long idServicio,
        String nombre,
        String tipo,
        BigDecimal precioBase
) {
}
