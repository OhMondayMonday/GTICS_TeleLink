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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('pendiente', 'confirmada', 'cancelada')")
    private Estado estado = Estado.pendiente;

    @Column(name = "razon_cancelacion")
    private String razonCancelacion;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum Estado {
        pendiente, confirmada, cancelada
    }
}