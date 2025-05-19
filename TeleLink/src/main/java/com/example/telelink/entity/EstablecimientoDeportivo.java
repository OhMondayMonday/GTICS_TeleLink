package com.example.telelink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    @NotBlank(message = "El nombre del establecimiento es obligatorio")
    @Size(max = 100, message = "El nombre del establecimiento no puede superar los 100 caracteres")
    private String establecimientoDeportivoNombre;

    @Column(name = "descripcion")
    @Size(max = 1000, message = "La descripción no debe superar los 1000 caracteres")
    private String descripcion;

    @Column(name = "direccion", nullable = false, length = 255)
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;

    @Column(name = "espacios_estacionamiento")
    @Min(value = 0, message = "Los espacios de estacionamiento no pueden ser negativos")
    @Max(value = 500, message = "No se permiten más de 500 espacios de estacionamiento")
    private Integer espaciosEstacionamiento;

    @Column(name = "telefono_contacto", length = 20)
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Pattern(regexp = "^\\d{1,20}$", message = "El teléfono debe contener solo números y hasta 20 dígitos")
    private String telefonoContacto;

    @Column(name = "correo_contacto", length = 100)
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
    private String correoContacto;

    @Column(name = "geolocalizacion", nullable = false, length = 255)
    @NotBlank(message = "La geolocalización es obligatoria")
    @Size(max = 255, message = "La geolocalización no puede superar los 255 caracteres")
    private String geolocalizacion;

    @Column(name = "foto_establecimiento_url", length = 255)
    @Size(max = 255, message = "La URL de la foto no puede superar los 255 caracteres")
    private String fotoEstablecimientoUrl;

    @Column(name = "horario_apertura", nullable = false)
    @NotNull(message = "El horario de apertura es obligatorio")
    private LocalTime horarioApertura;

    @Column(name = "horario_cierre", nullable = false)
    @NotNull(message = "El horario de cierre es obligatorio")
    private LocalTime horarioCierre;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.activo;

    @Column(name = "motivo_mantenimiento")
    @Size(max = 500, message = "El motivo de mantenimiento no puede superar los 500 caracteres")
    private String motivoMantenimiento;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum Estado {
        activo, clausurado, mantenimiento
    }
}