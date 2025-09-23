package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividad_usuarios")
@Getter
@Setter
public class ActividadUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actividad_id")
    private Integer actividadId;

    @ManyToOne
    @JoinColumn(name = "tipo_actividad_id", nullable = false)
    private TipoActividad tipoActividad;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 50)
    private String accion;

    private String detalles;

    @Column(name = "fecha_actividad")
    private LocalDateTime fechaActividad;

    @Column(name = "direccion_ip", length = 50)
    private String direccionIp;

    @Column(length = 255)
    private String ubicacion;

    @Column(length = 100)
    private String dispositivo;
}