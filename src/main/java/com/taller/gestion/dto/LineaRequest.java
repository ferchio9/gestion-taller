package com.taller.gestion.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Una linea que llega DENTRO del JSON de la orden. idServicio es opcional.
public record LineaRequest(
        Long idServicio,
        @NotBlank @Pattern(regexp = "MANO_OBRA|PIEZA",
                message = "tipo debe ser MANO_OBRA o PIEZA") String tipo,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal cantidad,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioUnitario
) {
}
