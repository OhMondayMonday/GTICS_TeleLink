package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipos_notificaciones")
@Getter
@Setter
public class TipoNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_notificacion_id")
    private Integer tipoNotificacionId;

    @Column(name = "tipo_notificacion", length = 50, unique = true)
    private String tipoNotificacion;
}