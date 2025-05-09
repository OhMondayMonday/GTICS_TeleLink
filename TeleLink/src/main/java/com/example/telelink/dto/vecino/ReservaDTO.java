package com.example.telelink.dto.vecino;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReservaDTO {
    private Integer servicioId;
    private String servicioNombre;
    private LocalDate fecha;
    private List<Integer> horas;
    private Integer numeroCarrilPiscina;
    private Integer numeroCarrilPista;
}
