package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversaciones")
@Getter
@Setter
public class Conversacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversacion_id")
    private Integer conversacionId;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "inicio_conversacion", updatable = false)
    private LocalDateTime inicioConversacion;

    @Column(name = "fin_conversacion")
    private LocalDateTime finConversacion;

    @Enumerated(EnumType.STRING)
    @Column(name="estado")
    private Estado estado = Estado.en_proceso;

    public enum Estado {
        en_proceso, finalizada
    }
}