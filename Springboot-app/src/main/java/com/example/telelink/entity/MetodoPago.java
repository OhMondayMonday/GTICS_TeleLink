package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "metodos_pago")
@Getter
@Setter
public class MetodoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metodo_pago_id")
    private Integer metodoPagoId;

    @Column(name = "metodo_pago", nullable = false, length = 50, unique = true)
    private String metodoPago;
}