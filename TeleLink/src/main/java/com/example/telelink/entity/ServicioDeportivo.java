package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "servicios_deportivos")
@Getter
@Setter
public class ServicioDeportivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "servicio_deportivo_id")
    private Integer servicioDeportivoId;

    @Column(name = "servicio_deportivo", nullable = false, length = 50, unique = true)
    private String servicioDeportivo;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}