package com.taller.gestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

// La orden se recibe con sus lineas en el mismo JSON.
// @Valid en la lista hace que se validen tambien las lineas.
public record OrdenRequest(
        @NotNull Long idVehiculo,
        @Size(max = 1000) String descripcionProblema,
        @PositiveOrZero Integer kmEntrada,
        @Pattern(regexp = "RECEPCION|DIAGNOSTICO|EN_REPARACION|LISTO|ENTREGADO",
                message = "estado no valido") String estado,
        @Valid List<LineaRequest> lineas
) {
}
