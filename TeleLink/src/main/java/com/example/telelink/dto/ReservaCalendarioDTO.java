package com.example.telelink.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservaCalendarioDTO {
    private String resourceId;
    private String title;
    private String start;
    private String end;
    private Integer numeroCarrilPiscina;
    private Integer numeroParticipantes;
    private String tipoServicio;

    public ReservaCalendarioDTO(String resourceId, String title, String start, String end) {
        this.resourceId = resourceId;
        this.title = title;
        this.start = start;
        this.end = end;
    }
    
    public ReservaCalendarioDTO(String resourceId, String title, String start, String end, 
                               Integer numeroCarrilPiscina, Integer numeroParticipantes, String tipoServicio) {
        this.resourceId = resourceId;
        this.title = title;
        this.start = start;
        this.end = end;
        this.numeroCarrilPiscina = numeroCarrilPiscina;
        this.numeroParticipantes = numeroParticipantes;
        this.tipoServicio = tipoServicio;
    }
}
