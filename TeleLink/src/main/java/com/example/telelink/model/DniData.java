package com.example.telelink.model;

public class DniData {
    private String numero;
    private String nombre_completo;
    private String nombres;
    private String apellido_paterno;
    private String apellido_materno;
    private int codigo_verificacion;
    private String ubigeo_sunat;
    private String[] ubigeo;
    private String direccion;

    // Getters y setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getNombre_completo() { return nombre_completo; }
    public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellido_paterno() { return apellido_paterno; }
    public void setApellido_paterno(String apellido_paterno) { this.apellido_paterno = apellido_paterno; }
    public String getApellido_materno() { return apellido_materno; }
    public void setApellido_materno(String apellido_materno) { this.apellido_materno = apellido_materno; }
    public int getCodigo_verificacion() { return codigo_verificacion; }
    public void setCodigo_verificacion(int codigo_verificacion) { this.codigo_verificacion = codigo_verificacion; }
    public String getUbigeo_sunat() { return ubigeo_sunat; }
    public void setUbigeo_sunat(String ubigeo_sunat) { this.ubigeo_sunat = ubigeo_sunat; }
    public String[] getUbigeo() { return ubigeo; }
    public void setUbigeo(String[] ubigeo) { this.ubigeo = ubigeo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}