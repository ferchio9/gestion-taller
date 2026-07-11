package com.taller.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @NotBlank
    @Size(max = 60)
    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    // Hash BCrypt (60 caracteres); nunca se guarda la contrasena en claro.
    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // ADMIN o MECANICO. Determina las autoridades de Spring Security (ver UsuarioDetailsService).
    // OJO: sin @NotBlank ni "nullable = false" a proposito. Hibernate traduce las anotaciones
    // de Bean Validation a restricciones NOT NULL en el DDL (hibernate.validator.apply_to_ddl),
    // y anadir una columna obligatoria con ALTER TABLE falla en Oracle si la tabla ya tiene
    // filas (ORA-01758). La validacion real de las peticiones vive en UsuarioRequest;
    // MigracionDatosRunner rellena esta columna en las filas creadas antes de que existiera.
    @Column(name = "rol", length = 15)
    private String rol;

    public Usuario() {
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
