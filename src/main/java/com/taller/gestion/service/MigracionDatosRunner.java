package com.taller.gestion.service;

import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Usuario;
import com.taller.gestion.repository.OrdenReparacionRepository;
import com.taller.gestion.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Rellena columnas añadidas despues de que ya hubiera filas en la base de datos
// (rol de Usuario, codigo_seguimiento de OrdenReparacion). Se dejaron nullable en
// el modelo justo para poder arrancar sin migraciones manuales; este runner
// completa los valores que falten cada vez que arranca (no hace nada si ya estan).
@Component
public class MigracionDatosRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final OrdenReparacionRepository ordenRepository;

    public MigracionDatosRunner(UsuarioRepository usuarioRepository, OrdenReparacionRepository ordenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.ordenRepository = ordenRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getRol() == null) {
                u.setRol("ADMIN");
                usuarioRepository.save(u);
            }
        }
        for (OrdenReparacion o : ordenRepository.findAll()) {
            if (o.getCodigoSeguimiento() == null) {
                o.setCodigoSeguimiento(UUID.randomUUID().toString());
                ordenRepository.save(o);
            }
        }
    }
}
