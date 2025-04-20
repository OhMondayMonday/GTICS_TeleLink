package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipos_actividades")
@Getter
@Setter
public class TipoActividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_actividad_id")
    private Integer tipoActividadId;

    @Column(name = "tipo_actividad", length = 100)
    private String tipoActividad;
}