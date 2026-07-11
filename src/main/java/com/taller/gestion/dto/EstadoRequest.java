package com.taller.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoRequest(
        @NotBlank
        @Pattern(regexp = "RECEPCION|DIAGNOSTICO|EN_REPARACION|LISTO|ENTREGADO",
                message = "estado no valido") String estado
) {
}
