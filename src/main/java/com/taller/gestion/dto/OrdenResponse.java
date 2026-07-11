package com.taller.gestion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenResponse(
        Long idOrden,
        Long idVehiculo,
        String matricula,
        String tipo,
        String estado,
        LocalDateTime fechaEntrada,
        LocalDateTime fechaSalida,
        String descripcionProblema,
        Integer kmEntrada,
        BigDecimal total,          // calculado: suma de importes de las lineas
        List<LineaResponse> lineas,
        String codigoSeguimiento,  // para componer el enlace publico /seguimiento.html?codigo=...
        Boolean presupuestoAprobado
) {
}
