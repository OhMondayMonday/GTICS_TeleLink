package com.example.telelink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones")
@Getter
@Setter
public class Observacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observacion_id")
    private Integer observacionId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @NotBlank(message = "La descripción es obligatoria")
    @Column
    private String descripcion;

    @NotBlank(message = "La foto es obligatoria al crear una observación")
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @NotNull(message = "El nivel de urgencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia")
    private NivelUrgencia nivelUrgencia;

    @NotNull(message = "El espacio deportivo es obligatorio")
    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;

    @NotNull(message = "El coordinador es obligatorio")
    @ManyToOne
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    @Column(name = "comentario_administrador")
    private String comentarioAdministrador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado;

    public enum NivelUrgencia {
        alto, medio, bajo
    }

    public enum Estado {
        pendiente, en_proceso, resuelto
    }
}