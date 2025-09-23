package com.example.telelink.dto.vecino;

// DTO para recibir los datos del pago
public class PagoRequest {
    private Double monto;
    private String email;
    private String descripcion;
    // getters y setters


    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
