package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "resenias")
@Getter
@Setter
public class Resenia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resenia_id")
    private Integer reseniaId;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer calificacion;

    @Column(length = 255)
    private String comentario;

    @Column(name = "foto_resenia_url", length = 255)
    private String fotoReseniaUrl;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id", nullable = false)
    private EspacioDeportivo espacioDeportivo;

    // Método para extraer un "título" del comentario
    public String getTituloResenia() {
        if (comentario == null || comentario.isEmpty()) {
            return "Reseña sin título";
        }
        // Tomar las primeras palabras del comentario como título
        return comentario.length() > 30 ?
                comentario.substring(0, 30) + "..." :
                comentario;
    }

    // Método para formatear la fecha (ej: "28 may 2025")
    public String getFechaFormateada() {
        if (this.fechaCreacion == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es"));
        return this.fechaCreacion.format(formatter);
    }

    public Integer getReseniaId() {
        return reseniaId;
    }

    public void setReseniaId(Integer reseniaId) {
        this.reseniaId = reseniaId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getFotoReseniaUrl() {
        return fotoReseniaUrl;
    }

    public void setFotoReseniaUrl(String fotoReseniaUrl) {
        this.fotoReseniaUrl = fotoReseniaUrl;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public EspacioDeportivo getEspacioDeportivo() {
        return espacioDeportivo;
    }

    public void setEspacioDeportivo(EspacioDeportivo espacioDeportivo) {
        this.espacioDeportivo = espacioDeportivo;
    }
}