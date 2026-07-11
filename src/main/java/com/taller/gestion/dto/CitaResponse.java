package com.taller.gestion.dto;

import java.time.LocalDateTime;

public record CitaResponse(
        Long idCita,
        Long idVehiculo,
        String matricula,
        String tipo,
        String nombreCliente,
        LocalDateTime fechaHora,
        String motivo,
        String estado,
        Long idOrdenGenerada
) {
}
