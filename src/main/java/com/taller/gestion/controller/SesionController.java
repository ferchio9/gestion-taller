package com.taller.gestion.controller;

import com.taller.gestion.dto.SesionResponse;
import com.taller.gestion.exception.RecursoNoEncontradoException;
import com.taller.gestion.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Le dice al frontend quien esta conectado y con que rol, para poder
// mostrar u ocultar botones y pantallas (borrar, gestion de usuarios, etc.).
@RestController
public class SesionController {

    private final UsuarioRepository usuarioRepository;

    public SesionController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/api/sesion")
    public SesionResponse sesion(Authentication authentication) {
        String username = authentication.getName();
        var usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + username));
        return new SesionResponse(usuario.getUsername(), usuario.getRol());
    }
}
