package com.taller.gestion.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Pattern(regexp = "MANO_OBRA|PIEZA",
                message = "tipo debe ser MANO_OBRA o PIEZA") String tipo,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioBase
) {
}
