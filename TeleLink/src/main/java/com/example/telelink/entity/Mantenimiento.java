package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimientos")
@Getter
@Setter
public class Mantenimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mantenimiento_id")
    private Integer mantenimientoId;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_estimada_fin")
    private LocalDateTime fechaEstimadaFin;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('pendiente', 'en_curso', 'finalizado')")
    private Estado estado;

    public enum Estado {
        pendiente, en_curso, finalizado
    }
}