package com.example.telelink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_deportivo_id", referencedColumnName = "espacio_deportivo_id")
    @NotNull(message = "Debe seleccionar un espacio deportivo")
    private EspacioDeportivo espacioDeportivo;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String motivo;

    @Column(name = "fecha_inicio")
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_estimada_fin")
    @NotNull(message = "La fecha estimada de fin es obligatoria")
    private LocalDateTime fechaEstimadaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.pendiente;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public enum Estado {
        pendiente,
        en_curso,
        finalizado
    }
}