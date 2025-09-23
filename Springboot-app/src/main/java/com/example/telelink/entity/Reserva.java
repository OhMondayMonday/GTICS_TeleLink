package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserva_id")
    private Integer reservaId;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "espacio_deportivo_id")
    private EspacioDeportivo espacioDeportivo;

    @Column(name = "inicio_reserva", nullable = false)
    private LocalDateTime inicioReserva;

    @Column(name = "fin_reserva", nullable = false)
    private LocalDateTime finReserva;

    @Column(name = "numero_carril_piscina")
    private Integer numeroCarrilPiscina;

    @Column(name = "numero_carril_pista")
    private Integer numeroCarrilPista;
    
    @Column(name = "numero_participantes")
    private Integer numeroParticipantes = 1; // Valor por defecto es 1

    @Convert(converter = EstadoConverter.class)
    @Column(name = "estado", nullable = false)
    private Estado estado = Estado.pendiente;

    @Column(name = "razon_cancelacion")
    private String razonCancelacion;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    public enum Estado {
        pendiente, confirmada, cancelada, completada, en_proceso
    }

    @Converter(autoApply = true)
    public static class EstadoConverter implements AttributeConverter<Estado, String> {
        @Override
        public String convertToDatabaseColumn(Estado estado) {
            return estado == null ? null : estado.name();
        }

        @Override
        public Estado convertToEntityAttribute(String dbData) {
            return dbData == null ? null : Estado.valueOf(dbData);
        }
    }


    public Integer getReservaId() {
        return reservaId;
    }

    public void setReservaId(Integer reservaId) {
        this.reservaId = reservaId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EspacioDeportivo getEspacioDeportivo() {
        return espacioDeportivo;
    }

    public void setEspacioDeportivo(EspacioDeportivo espacioDeportivo) {
        this.espacioDeportivo = espacioDeportivo;
    }

    public LocalDateTime getInicioReserva() {
        return inicioReserva;
    }

    public void setInicioReserva(LocalDateTime inicioReserva) {
        this.inicioReserva = inicioReserva;
    }

    public LocalDateTime getFinReserva() {
        return finReserva;
    }

    public void setFinReserva(LocalDateTime finReserva) {
        this.finReserva = finReserva;
    }

    public Integer getNumeroCarrilPiscina() {
        return numeroCarrilPiscina;
    }

    public void setNumeroCarrilPiscina(Integer numeroCarrilPiscina) {
        this.numeroCarrilPiscina = numeroCarrilPiscina;
    }

    public Integer getNumeroCarrilPista() {
        return numeroCarrilPista;
    }

    public void setNumeroCarrilPista(Integer numeroCarrilPista) {
        this.numeroCarrilPista = numeroCarrilPista;
    }
    
    public Integer numeroParticipantes() {
        return numeroParticipantes;
    }

    public void setNumeroParticipantesPiscina(Integer numeroParticipantesPiscina) {
        this.numeroParticipantes = numeroParticipantesPiscina;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getRazonCancelacion() {
        return razonCancelacion;
    }

    public void setRazonCancelacion(String razonCancelacion) {
        this.razonCancelacion = razonCancelacion;
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
}