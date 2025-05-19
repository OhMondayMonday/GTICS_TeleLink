package com.example.telelink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no debe superar los 255 caracteres")
    @Column(nullable = false, length = 255)
    private String nombre;

    @NotNull(message = "El servicio deportivo es obligatorio")
    @ManyToOne
    @JoinColumn(name = "servicio_deportivo_id", nullable = false)
    private ServicioDeportivo servicioDeportivo;

    @NotNull(message = "El establecimiento deportivo es obligatorio")
    @ManyToOne
    @JoinColumn(name = "establecimiento_deportivo_id", nullable = false)
    private EstablecimientoDeportivo establecimientoDeportivo;

    @Min(value = 1, message = "Debe haber al menos una persona por carril")
    @Max(value = 10, message = "No puede haber más de 10 personas por carril")
    @Column(name = "max_personas_por_carril")
    private Integer maxPersonasPorCarril;

    @Min(value = 1, message = "Debe haber al menos un carril")
    @Max(value = 10, message = "No puede haber más de 10 carriles")
    @Column(name = "carriles_piscina")
    private Integer carrilesPiscina;

    @Min(value = 1, message = "La longitud mínima es 1 metro")
    @Max(value = 200, message = "La longitud máxima es 200 metros")
    @Column(name = "longitud_piscina")
    private Integer longitudPiscina;

    @DecimalMin(value = "0.10", message = "La profundidad mínima es 0.10 metros")
    @DecimalMax(value = "50.00", message = "La profundidad máxima es 50 metros")
    @Column(name = "profundidad_piscina", precision = 6, scale = 2)
    private BigDecimal profundidadPiscina;

    @Size(max = 1000, message = "La descripción no debe exceder los 1000 caracteres")
    @Lob
    private String descripcion;

    @Column(name = "foto_espacio_deportivo_url", length = 255)
    private String fotoEspacioDeportivoUrl;


    @Min(value = 1, message = "El aforo debe ser al menos 1")
    @Max(value = 200, message = "El aforo no debe superar 200 personas")
    @Column(name = "aforo_gimnasio")
    private Integer aforoGimnasio;

    @DecimalMin(value = "1.00", message = "La longitud mínima de la pista es 1 metro")
    @DecimalMax(value = "1000.00", message = "La longitud máxima de la pista es 1000 metros")
    @Column(name = "longitud_pista", precision = 6, scale = 2)
    private BigDecimal longitudPista;

    @Min(value = 1, message = "Debe haber al menos un carril")
    @Max(value = 50, message = "No puede haber más de 50 carriles")
    @Column(name = "carriles_pista")
    private Integer carrilesPista;

    /*
    @Size(max = 255, message = "La geolocalización no debe superar los 255 caracteres")
    @Pattern(regexp = "^[-+]?\\d{1,3}\\.\\d+,-?\\d{1,3}\\.\\d+$", message = "Formato inválido (ej: -12.04318,-77.02824)")
    @Column(name = "geolocalizacion", length = 255)
    private String geolocalizacion;
    */
    @Size(max = 255, message = "La geolocalización no debe superar los 255 caracteres")
    @Pattern(
            regexp = "^[-+]?\\d{1,3}\\.\\d+,-?\\d{1,3}\\.\\d+$",
            message = "Formato inválido (ej: -12.04318,-77.02824)"
    )
    @Column(name = "geolocalizacion", length = 255)
    private String geolocalizacion;

    @NotNull(message = "El estado del servicio es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_servicio")
    private EstadoServicio estadoServicio;


    @Size(max = 9, message = "El número de soporte debe tener como máximo 9 caracteres")
    @Pattern(regexp = "^\\d{0,9}$", message = "Solo se permiten números de hasta 9 dígitos")
    @Column(name = "numero_soporte", length = 9)
    private String numeroSoporte;

    @NotNull(message = "El horario de apertura es obligatorio")
    @Column(name = "horario_apertura")
    private LocalTime horarioApertura;

    @NotNull(message = "El horario de cierre es obligatorio")
    @Column(name = "horario_cierre")
    private LocalTime horarioCierre;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @DecimalMin(value = "0.00", message = "El precio por hora no puede ser negativo")
    @DecimalMax(value = "9999.99", message = "El precio por hora no puede superar 9999.99")
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