package com.taller.gestion.service;

import com.taller.gestion.dto.UsuarioRequest;
import com.taller.gestion.dto.UsuarioResponse;
import com.taller.gestion.exception.ConflictoException;
import com.taller.gestion.model.Usuario;
import com.taller.gestion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void prepararService() {
        service = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void crearGuardaElUsuarioConLaContrasenaCifrada() {
        UsuarioRequest req = new UsuarioRequest("nuevo", "password123", "MECANICO");

        when(usuarioRepository.findByUsername("nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hash-bcrypt-simulado");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1L);
            return u;
        });

        UsuarioResponse resultado = service.crear(req);

        assertThat(resultado.username()).isEqualTo("nuevo");
        assertThat(resultado.rol()).isEqualTo("MECANICO");
        verify(usuarioRepository).save(argThat(u -> u.getPasswordHash().equals("hash-bcrypt-simulado")));
    }

    @Test
    void crearLanzaConflictoSiElUsernameYaExiste() {
        UsuarioRequest req = new UsuarioRequest("existente", "password123", "MECANICO");
        when(usuarioRepository.findByUsername("existente"))
                .thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(ConflictoException.class);
    }
}
