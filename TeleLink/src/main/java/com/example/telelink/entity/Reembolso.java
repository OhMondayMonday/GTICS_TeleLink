package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reembolsos")
@Getter
@Setter
public class Reembolso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reembolso_id")
    private Integer reembolsoId;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('pendiente', 'completado', 'rechazado', 'cancelado')")
    private Estado estado = Estado.pendiente;

    private String motivo;

    @Column(name = "fecha_reembolso")
    private LocalDateTime fechaReembolso;

    @Column(name = "foto_comprobacion_reembolso_url", length = 255)
    private String fotoComprobacionReembolsoUrl;

    @Column(name = "detalles_transaccion")
    private String detallesTransaccion;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    public enum Estado {
        pendiente, completado, rechazado, cancelado
    }
}