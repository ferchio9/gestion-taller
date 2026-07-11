package com.taller.gestion.dto;

import jakarta.validation.constraints.NotNull;

public record CitaConvertirRequest(
        @NotNull Long idOrden
) {
}
