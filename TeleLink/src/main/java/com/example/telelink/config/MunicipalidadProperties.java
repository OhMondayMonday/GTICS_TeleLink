package com.example.telelink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "municipalidad")
public class MunicipalidadProperties {

    private String nombre;
    private String telefono;
    private String telefonoEmergencias;
    private String whatsapp;
    private String web;
    private String email;
    private String direccion;
    private Horario horario = new Horario();

    // Getters y setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTelefonoEmergencias() { return telefonoEmergencias; }
    public void setTelefonoEmergencias(String telefonoEmergencias) { this.telefonoEmergencias = telefonoEmergencias; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getWeb() { return web; }
    public void setWeb(String web) { this.web = web; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Horario getHorario() { return horario; }
    public void setHorario(Horario horario) { this.horario = horario; }

    public static class Horario {
        private String atencion;

        public String getAtencion() { return atencion; }
        public void setAtencion(String atencion) { this.atencion = atencion; }
    }
}