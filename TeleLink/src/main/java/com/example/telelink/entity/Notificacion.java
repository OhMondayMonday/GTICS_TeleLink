package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Integer notificacionId;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo_notificacion", length = 50)
    private String tituloNotificacion;

    private String mensaje;

    @Column(name = "url_redireccion", length = 255)
    private String urlRedireccion;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('no_leido', 'leido')")
    private Estado estado = Estado.no_leido;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "tipo_notificacion_id", nullable = false)
    private TipoNotificacion tipoNotificacion;

    public enum Estado {
        no_leido, leido
    }
}