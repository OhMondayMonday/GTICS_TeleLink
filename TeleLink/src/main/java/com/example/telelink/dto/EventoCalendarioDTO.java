package com.example.telelink.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoCalendarioDTO {
    private String inicio;
    private String fin;
    private String coordinador;
    private String estado;
    private String horario;
    private Boolean tieneAsistenciaSolapada;
    private Integer espacioDeportivoId;
    private String espacioDeportivoNombre;

    // Constructor para mantener compatibilidad con código existente
    public EventoCalendarioDTO(String inicio, String fin, String coordinador, String estado, String horario) {
        this.inicio = inicio;
        this.fin = fin;
        this.coordinador = coordinador;
        this.estado = estado;
        this.horario = horario;
        this.tieneAsistenciaSolapada = false;
        this.espacioDeportivoId = null;
        this.espacioDeportivoNombre = null;
    }
}
