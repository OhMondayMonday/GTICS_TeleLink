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
}