package com.taller.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Opcion A: el vehiculo se asocia al cliente por su id.
public record VehiculoRequest(
        @NotBlank @Size(max = 15) String matricula,
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 50) String modelo,
        Short anio,
        @Size(max = 20) String bastidorVin,
        Integer kmActual,
        @NotBlank @Pattern(regexp = "COCHE|FURGONETA|MOTOCICLETA|CAMION",
                message = "tipo de vehículo no válido") String tipo,
        @NotNull Long idCliente
) {
}
