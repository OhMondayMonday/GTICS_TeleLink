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
}
