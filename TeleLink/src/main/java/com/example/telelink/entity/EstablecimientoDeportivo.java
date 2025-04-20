package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "establecimientos_deportivos")
@Getter
@Setter
public class EstablecimientoDeportivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "establecimiento_deportivo_id")
    private Integer establecimientoDeportivoId;

    @Column(name = "establecimiento_deportivo", nullable = false, length = 100, unique = true)
    private String establecimientoDeportivo;

    private String descripcion;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(name = "espacios_estacionamiento")
    private Integer espaciosEstacionamiento;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "correo_contacto", length = 100)
    private String correoContacto;

    @Column(nullable = false, length = 255)
    private String geolocalizacion;

    @Column(name = "foto_establecimiento_url", length = 255)
    private String fotoEstablecimientoUrl;

    @Column(name = "horario_apertura", nullable = false)
    private LocalTime horarioApertura;

    @Column(name = "horario_cierre", nullable = false)
    private LocalTime horarioCierre;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('activo', 'clausurado', 'mantenimiento')")
    private Estado estado = Estado.activo;

    @Column(name = "motivo_mantenimiento")
    private String motivoMantenimiento;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum Estado {
        activo, clausurado, mantenimiento
    }
}