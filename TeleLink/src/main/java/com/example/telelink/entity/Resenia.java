package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "resenias")
@Getter
@Setter
public class Resenia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resenia_id")
    private Integer reseniaId;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer calificacion;

    @Column(length = 255)
    private String comentario;

    @Column(name = "foto_resenia_url", length = 255)
    private String fotoReseniaUrl;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;
}