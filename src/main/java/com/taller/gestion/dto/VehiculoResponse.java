package com.taller.gestion.dto;

// El cliente va "aplanado" (id + nombre), no como objeto anidado.
public record VehiculoResponse(
        Long idVehiculo,
        String matricula,
        String marca,
        String modelo,
        Short anio,
        String bastidorVin,
        Integer kmActual,
        String tipo,
        Long idCliente,
        String nombreCliente
) {
}
