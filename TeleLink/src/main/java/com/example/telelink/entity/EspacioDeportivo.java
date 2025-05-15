package com.example.telelink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "espacios_deportivos")
@Getter
@Setter
public class EspacioDeportivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "espacio_deportivo_id")
    private Integer espacioDeportivoId;

    @Column(nullable = false, length = 255)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "servicio_deportivo_id", nullable = false)
    private ServicioDeportivo servicioDeportivo;

    @ManyToOne
    @JoinColumn(name = "establecimiento_deportivo_id", nullable = false)
    private EstablecimientoDeportivo establecimientoDeportivo;

    @Column(name = "max_personas_por_carril")
    private Integer maxPersonasPorCarril;

    @Column(name = "carriles_piscina")
    private Integer carrilesPiscina;

    @Column(name = "longitud_piscina")
    private Integer longitudPiscina;

    @Column(name = "profundidad_piscina", precision = 6, scale = 2)
    private BigDecimal profundidadPiscina;

    @Lob
    private String descripcion;

    @Column(name = "foto_espacio_deportivo_url", length = 255)
    private String fotoEspacioDeportivoUrl;

    @Column(name = "aforo_gimnasio")
    private Integer aforoGimnasio;

    @Column(name = "longitud_pista", precision = 6, scale = 2)
    private BigDecimal longitudPista;

    @Column(name = "carriles_pista")
    private Integer carrilesPista;

    @Column(name = "geolocalizacion", length = 255)
    private String geolocalizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_servicio")
    private EstadoServicio estadoServicio;

    @Column(name = "numero_soporte", length = 9)
    private String numeroSoporte;

    @Column(name = "horario_apertura")
    private LocalTime horarioApertura;

    @Column(name = "horario_cierre")
    private LocalTime horarioCierre;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name="precio_por_hora", precision = 6, scale = 2)
    private BigDecimal precioPorHora;

    public enum EstadoServicio {
        operativo, mantenimiento, clausurado
    }

    public Integer getEspacioDeportivoId() {
        return espacioDeportivoId;
    }

    public void setEspacioDeportivoId(Integer espacioDeportivoId) {
        this.espacioDeportivoId = espacioDeportivoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public ServicioDeportivo getServicioDeportivo() {
        return servicioDeportivo;
    }

    public void setServicioDeportivo(ServicioDeportivo servicioDeportivo) {
        this.servicioDeportivo = servicioDeportivo;
    }

    public EstablecimientoDeportivo getEstablecimientoDeportivo() {
        return establecimientoDeportivo;
    }

    public void setEstablecimientoDeportivo(EstablecimientoDeportivo establecimientoDeportivo) {
        this.establecimientoDeportivo = establecimientoDeportivo;
    }

    public Integer getMaxPersonasPorCarril() {
        return maxPersonasPorCarril;
    }

    public void setMaxPersonasPorCarril(Integer maxPersonasPorCarril) {
        this.maxPersonasPorCarril = maxPersonasPorCarril;
    }

    public Integer getCarrilesPiscina() {
        return carrilesPiscina;
    }

    public void setCarrilesPiscina(Integer carrilesPiscina) {
        this.carrilesPiscina = carrilesPiscina;
    }

    public Integer getLongitudPiscina() {
        return longitudPiscina;
    }

    public void setLongitudPiscina(Integer longitudPiscina) {
        this.longitudPiscina = longitudPiscina;
    }

    public BigDecimal getProfundidadPiscina() {
        return profundidadPiscina;
    }

    public void setProfundidadPiscina(BigDecimal profundidadPiscina) {
        this.profundidadPiscina = profundidadPiscina;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFotoEspacioDeportivoUrl() {
        return fotoEspacioDeportivoUrl;
    }

    public void setFotoEspacioDeportivoUrl(String fotoEspacioDeportivoUrl) {
        this.fotoEspacioDeportivoUrl = fotoEspacioDeportivoUrl;
    }

    public Integer getAforoGimnasio() {
        return aforoGimnasio;
    }

    public void setAforoGimnasio(Integer aforoGimnasio) {
        this.aforoGimnasio = aforoGimnasio;
    }

    public BigDecimal getLongitudPista() {
        return longitudPista;
    }

    public void setLongitudPista(BigDecimal longitudPista) {
        this.longitudPista = longitudPista;
    }

    public Integer getCarrilesPista() {
        return carrilesPista;
    }

    public void setCarrilesPista(Integer carrilesPista) {
        this.carrilesPista = carrilesPista;
    }

    public String getGeolocalizacion() {
        return geolocalizacion;
    }

    public void setGeolocalizacion(String geolocalizacion) {
        this.geolocalizacion = geolocalizacion;
    }

    public EstadoServicio getEstadoServicio() {
        return estadoServicio;
    }

    public void setEstadoServicio(EstadoServicio estadoServicio) {
        this.estadoServicio = estadoServicio;
    }

    public String getNumeroSoporte() {
        return numeroSoporte;
    }

    public void setNumeroSoporte(String numeroSoporte) {
        this.numeroSoporte = numeroSoporte;
    }

    public LocalTime getHorarioApertura() {
        return horarioApertura;
    }

    public void setHorarioApertura(LocalTime horarioApertura) {
        this.horarioApertura = horarioApertura;
    }

    public LocalTime getHorarioCierre() {
        return horarioCierre;
    }

    public void setHorarioCierre(LocalTime horarioCierre) {
        this.horarioCierre = horarioCierre;
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

    public BigDecimal getPrecioPorHora() {
        return precioPorHora;
    }

    public void setPrecioPorHora(BigDecimal precioPorHora) {
        this.precioPorHora = precioPorHora;
    }
}