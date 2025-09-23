package com.example.telelink.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AsistenciaForm {
    private Integer coordinadorId;
    private Integer espacioDeportivoId;
    private String fecha;
    private String horarioEntrada;
    private String horarioSalida;
}