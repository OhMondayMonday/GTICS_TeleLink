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

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transaccion")
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

    public Integer getPagoId() {
        return pagoId;
    }

    public void setPagoId(Integer pagoId) {
        this.pagoId = pagoId;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public EstadoTransaccion getEstadoTransaccion() {
        return estadoTransaccion;
    }

    public void setEstadoTransaccion(EstadoTransaccion estadoTransaccion) {
        this.estadoTransaccion = estadoTransaccion;
    }

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }

    public String getFotoComprobanteUrl() {
        return fotoComprobanteUrl;
    }

    public void setFotoComprobanteUrl(String fotoComprobanteUrl) {
        this.fotoComprobanteUrl = fotoComprobanteUrl;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getDetallesTransaccion() {
        return detallesTransaccion;
    }

    public void setDetallesTransaccion(String detallesTransaccion) {
        this.detallesTransaccion = detallesTransaccion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}