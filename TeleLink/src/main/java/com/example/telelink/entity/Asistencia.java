package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias")
@Getter
@Setter
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asistencia_id")
    private Integer asistenciaId;

    @ManyToOne
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuario administrador;

    @ManyToOne
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    @Column(name = "horario_entrada")
    private LocalDateTime horarioEntrada;

    @Column(name = "horario_salida")
    private LocalDateTime horarioSalida;

    @Column(name = "registro_entrada")
    private LocalDateTime registroEntrada;

    @Column(name = "registro_salida")
    private LocalDateTime registroSalida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrada")
    private EstadoEntrada estadoEntrada = EstadoEntrada.pendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_salida")
    private EstadoSalida estadoSalida;

    @Column(length = 100)
    private String geolocalizacion;

    @Column(name = "observacion_asistencia")
    private String observacionAsistencia;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;

    public enum EstadoEntrada {
        puntual, tarde, pendiente, inasistencia
    }

    public enum EstadoSalida {
        realizado, pendiente, inasistencia
    }
}