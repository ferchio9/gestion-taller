package com.taller.gestion.service;

import com.taller.gestion.dto.UsuarioRequest;
import com.taller.gestion.dto.UsuarioResponse;
import com.taller.gestion.exception.ConflictoException;
import com.taller.gestion.model.Usuario;
import com.taller.gestion.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (usuarioRepository.findByUsername(req.username()).isPresent()) {
            throw new ConflictoException("Ya existe un usuario con ese nombre.");
        }
        Usuario u = new Usuario();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRol(req.rol());
        Usuario guardado = usuarioRepository.save(u);
        log.info("Usuario creado: id={}, username={}, rol={}",
                guardado.getIdUsuario(), guardado.getUsername(), guardado.getRol());
        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getIdUsuario(), u.getUsername(), u.getRol());
    }
}
