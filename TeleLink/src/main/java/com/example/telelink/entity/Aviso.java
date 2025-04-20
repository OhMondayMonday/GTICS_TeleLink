package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "avisos")
@Getter
@Setter
public class Aviso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aviso_id")
    private Integer avisoId;

    @Column(name = "titulo_aviso", nullable = false, length = 50)
    private String tituloAviso;

    @Column(name = "texto_aviso", nullable = false)
    private String textoAviso;

    @Column(name = "foto_aviso_url", nullable = false, length = 255)
    private String fotoAvisoUrl;

    @Column(name = "fecha_aviso")
    private LocalDateTime fechaAviso;
}