package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pago_id")
    private Integer pagoId;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transaccion", columnDefinition = "ENUM('completado', 'fallido', 'pendiente')")
    private EstadoTransaccion estadoTransaccion = EstadoTransaccion.pendiente;

    @Column(name = "transaccion_id", length = 255, unique = true)
    private String transaccionId;

    @Column(name = "foto_comprobante_url", length = 255)
    private String fotoComprobanteUrl;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "detalles_transaccion")
    private String detallesTransaccion;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    public enum EstadoTransaccion {
        completado, fallido, pendiente
    }
}