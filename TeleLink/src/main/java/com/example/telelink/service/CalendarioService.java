package com.example.telelink.service;

import com.example.telelink.dto.EventoCalendarioDTO;
import com.example.telelink.entity.Asistencia;
import com.example.telelink.entity.Reserva;
import com.example.telelink.repository.AsistenciaRepository;
import com.example.telelink.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CalendarioService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    public List<EventoCalendarioDTO> obtenerReservasConsolidadas(Integer espacioId, LocalDateTime inicio, LocalDateTime fin) {
        List<Reserva> reservas = reservaRepository.findReservasEnRango(espacioId, inicio, fin);
        
        // Ordenar las reservas por fecha de inicio
        reservas.sort(Comparator.comparing(Reserva::getInicioReserva));

        List<EventoCalendarioDTO> eventos = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Consolidar horarios que se solapan
        if (!reservas.isEmpty()) {
            LocalDateTime inicioActual = reservas.get(0).getInicioReserva();
            LocalDateTime finActual = reservas.get(0).getFinReserva();

            for (int i = 1; i < reservas.size(); i++) {
                Reserva reservaActual = reservas.get(i);

                // Si la reserva actual comienza antes o al mismo tiempo que termina la anterior
                if (!reservaActual.getInicioReserva().isAfter(finActual)) {
                    // Extender el fin si la reserva actual termina después
                    if (reservaActual.getFinReserva().isAfter(finActual)) {
                        finActual = reservaActual.getFinReserva();
                    }
                } else {
                    // No hay solapamiento, guardar el periodo actual y comenzar uno nuevo
                    eventos.add(new EventoCalendarioDTO(
                        inicioActual.toString(),
                        finActual.toString(),
                        null,
                        null,
                        inicioActual.format(formatter) + " - " + finActual.format(formatter)
                    ));

                    inicioActual = reservaActual.getInicioReserva();
                    finActual = reservaActual.getFinReserva();
                }
            }

            // Añadir el último periodo
            eventos.add(new EventoCalendarioDTO(
                inicioActual.toString(),
                finActual.toString(),
                null,
                null,
                inicioActual.format(formatter) + " - " + finActual.format(formatter)
            ));
        }

        return eventos;
    }

    public List<EventoCalendarioDTO> obtenerAsistencias(Integer espacioId, LocalDateTime inicio, LocalDateTime fin) {
        List<Asistencia> asistencias = asistenciaRepository.findAsistenciasEnRango(espacioId, inicio, fin);
        List<EventoCalendarioDTO> eventos = new ArrayList<>();
        
        for (Asistencia asistencia : asistencias) {
            eventos.add(new EventoCalendarioDTO(
                asistencia.getHorarioEntrada().toString(),
                asistencia.getHorarioSalida().toString(),
                asistencia.getCoordinador().getNombres() + " " + asistencia.getCoordinador().getApellidos(),
                asistencia.getEstadoEntrada().toString(),
                null
            ));
        }

        return eventos;
    }
}
