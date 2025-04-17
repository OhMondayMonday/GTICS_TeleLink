package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones")
@Getter
@Setter
public class Observacion {
    @Id
    @Column(name = "observacion_id")
    private Integer observacionId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    private String descripcion;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia", columnDefinition = "ENUM('alto', 'medio', 'bajo')")
    private NivelUrgencia nivelUrgencia;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;

    @ManyToOne
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    public enum NivelUrgencia {
        alto, medio, bajo
    }
}