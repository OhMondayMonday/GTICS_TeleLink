package com.example.telelink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 100)
    @Column(name = "correo_electronico", nullable = false, length = 100, unique = true)
    private String correoElectronico;

    @Column(name = "contrasenia_hash", nullable = false, length = 255)
    private String contraseniaHash;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(length = 8, unique = true)
    private String dni;

    @Size(max = 255, message = "Máximo 255 caracteres")
    private String direccion;

    @Size(min = 9, max = 9, message = "El teléfono debe tener exactamente 9 dígitos")
    @Pattern(regexp = "^[0-9]+$", message = "El teléfono solo puede contener números")
    private String telefono;

    @Column(name = "foto_perfil_url", length = 255)
    private String fotoPerfilUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta")
    private EstadoCuenta estadoCuenta = EstadoCuenta.pendiente;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum EstadoCuenta {
        activo, eliminado, baneado, pendiente
    }
}