package com.taller.gestion.dto;

import java.time.LocalDateTime;

public record CambioEstadoResponse(
        LocalDateTime fecha,
        String estadoAnterior,
        String estadoNuevo,
        String usuario
) {
}
