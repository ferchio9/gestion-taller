package com.taller.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que la API acepta al crear/actualizar un cliente.
public record ClienteRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 20) String telefono,
        @Email @Size(max = 150) String email,
        @Size(max = 15) String nifCif
) {
}
