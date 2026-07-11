package com.taller.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank
        @Size(max = 60)
        String username,

        @NotBlank
        @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres")
        String password,

        @NotBlank
        @Pattern(regexp = "ADMIN|MECANICO", message = "rol no valido")
        String rol
) {
}
