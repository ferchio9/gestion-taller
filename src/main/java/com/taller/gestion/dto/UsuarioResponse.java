package com.taller.gestion.dto;

// No incluye passwordHash: nunca se expone, ni siquiera al admin.
public record UsuarioResponse(
        Long idUsuario,
        String username,
        String rol
) {
}
