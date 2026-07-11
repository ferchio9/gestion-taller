package com.taller.gestion.dto;

import jakarta.validation.constraints.NotNull;

public record AprobacionRequest(
        @NotNull Boolean aprobado
) {
}
