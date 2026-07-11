package com.taller.gestion.dto;

import java.time.LocalDate;

// Lo que la API devuelve de un cliente.
public record ClienteResponse(
        Long idCliente,
        String nombre,
        String telefono,
        String email,
        String nifCif,
        LocalDate fechaAlta
) {
}
