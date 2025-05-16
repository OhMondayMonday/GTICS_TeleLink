package com.example.telelink.controller;

import com.example.telelink.dto.ReservaCalendarioDTO;
import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.entity.Reserva;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @GetMapping("/inicio")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAllByOrderByUsuarioIdAsc());
        return "lista-usuarios";
    }

    @GetMapping("/calendario")
    public String verCalendario() {
        return "vecino/reservasVecino";
    }

    @GetMapping("/api/espacios")
    @ResponseBody
    public List<?> obtenerEspacios() {
        return espacioDeportivoRepository.findAll().stream()
                .map(e -> new Object() {
                    public final String id = e.getEspacioDeportivoId().toString();
                    public final String title = e.getNombre();
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/reservas")
    @ResponseBody
    public List<ReservaCalendarioDTO> obtenerReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        if (fecha == null) fecha = LocalDate.now();
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);

        return reservaRepository.findByInicioReservaBetween(inicio, fin).stream()
                .map(r -> new ReservaCalendarioDTO(
                        r.getEspacioDeportivo().getEspacioDeportivoId().toString(),
                        "Reservado",
                        r.getInicioReserva().toString(),
                        r.getFinReserva().toString()
                )).collect(Collectors.toList());
    }
}
