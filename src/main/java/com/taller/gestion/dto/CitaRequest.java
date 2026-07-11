package com.taller.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull Long idVehiculo,
        @NotNull LocalDateTime fechaHora,
        @Size(max = 200) String motivo,

        @Pattern(regexp = "PENDIENTE|CONFIRMADA|CANCELADA|COMPLETADA", message = "estado no valido")
        String estado
) {
}
