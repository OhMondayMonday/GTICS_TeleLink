package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(name="correo_electronico", nullable = false, length = 100, unique = true)
    private String correoElectronico;

    @Column(name="contrasenia_hash", nullable = false, length = 255)
    private String contraseniaHash;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(length = 8, unique = true)
    private String dni;

    @Column(length = 255)
    private String direccion;

    @Column(length = 9)
    private String telefono;

    @Column(name = "foto_perfil_url", length = 255)
    private String fotoPerfilUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", columnDefinition = "ENUM('activo','eliminado','baneado', 'pendiente') default 'pendiente'")
    private EstadoCuenta estadoCuenta;

    @Column(name="fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum EstadoCuenta {
        activo, eliminado, baneado, pendiente
    }
}