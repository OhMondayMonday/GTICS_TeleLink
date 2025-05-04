package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mensaje_id")
    private Integer mensajeId;

    @ManyToOne
    @JoinColumn(name = "conversacion_id")
    private Conversacion conversacion;

    @Column(name = "texto_mensaje", nullable = false)
    private String textoMensaje;

    @Enumerated(EnumType.STRING)
    private Origen origen = Origen.usuario;

    @Column
    private LocalDateTime fecha;

    public enum Origen {
        usuario, chatbot
    }
}