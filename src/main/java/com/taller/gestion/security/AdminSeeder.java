package com.taller.gestion.security;

import com.taller.gestion.model.Usuario;
import com.taller.gestion.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Crea el usuario admin la primera vez que arranca la aplicacion (tabla usuario vacia).
// Las credenciales iniciales se definen en application.properties: cambialas antes de desplegar.
@Component
public class AdminSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsuario;
    private final String adminPassword;

    public AdminSeeder(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.admin.usuario}") String adminUsuario,
                        @Value("${app.admin.password}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsuario = adminUsuario;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(adminUsuario);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRol("ADMIN");
        usuarioRepository.save(admin);
    }
}
