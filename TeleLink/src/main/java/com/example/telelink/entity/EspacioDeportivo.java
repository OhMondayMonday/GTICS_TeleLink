package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "espacios_deportivos")
@Getter
@Setter
public class EspacioDeportivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="espacio_deportivo_id")
    private Integer espacioDeportivoId;

    @Column(nullable = false, length = 255)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "servicio_deportivo_id", nullable = false)
    private ServicioDeportivo servicioDeportivo;

    @ManyToOne
    @JoinColumn(name = "establecimiento_deportivo_id", nullable = false)
    private EstablecimientoDeportivo establecimientoDeportivo;

    @Column(name = "max_personas_por_carril")
    private Integer maxPersonasPorCarril;

    @Column(name = "carriles_piscina")
    private Integer carrilesPiscina;

    @Column(name = "longitud_piscina")
    private Integer longitudPiscina;

    @Column(name = "profundidad_piscina", precision = 4, scale = 2)
    private BigDecimal profundidadPiscina;

    private String descripcion;

    @Column(name = "aforo_gimnasio")
    private Integer aforoGimnasio;

    @Column(name = "longitud_pista", precision = 4, scale = 2)
    private BigDecimal longitudPista;

    @Column(name = "carriles_pista")
    private Integer carrilesPista;

    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_servicio", columnDefinition = "ENUM('operativo', 'mantenimiento', 'clausurado')")
    private EstadoServicio estadoServicio;

    @Column(name = "numero_soporte", length = 9)
    private String numeroSoporte;

    @Column(name = "horario_apertura")
    private LocalTime horarioApertura;

    @Column(name = "horario_cierre")
    private LocalTime horarioCierre;

    @Column(name="fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum EstadoServicio {
        operativo, mantenimiento, clausurado
    }
}