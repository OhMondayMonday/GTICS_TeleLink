package com.example.telelink.controller;

import com.example.telelink.dto.Superadmin.AvisoDTO;
import com.example.telelink.entity.Aviso;
import com.example.telelink.repository.AvisoRepository;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private AvisoRepository avisoRepository;
    @Autowired
    private PagoRepository pagoRepository;

    @GetMapping("/inicio")
    public String dashboard(Model model) {

        // Obtener los datos de reservas y pagos
        long numeroReservasMes = reservaRepository.numeroReservasMes();
        long numeroReservasMesPasado = reservaRepository.numeroReservasMesPasado();
        Double montoMensual = reservaRepository.obtenerMontoTotalDeReservasEsteMes();
        Double promedioMensual = montoMensual/numeroReservasMes;
        Double montoMensualPasado = reservaRepository.obtenerMontoTotalDeReservasMesPasado();
        List<Aviso> avisos = avisoRepository.obtenerUltimosAvisos();
        Aviso ultimoAviso = avisoRepository.findByEstadoAviso("activo");
        Integer reservasFutbol = reservaRepository.obtenerNumeroReservasPorServicio(2);
        Integer reservasPiscina = reservaRepository.obtenerNumeroReservasPorServicio(4);
        Integer reservasGimnasio = reservaRepository.obtenerNumeroReservasPorServicio(1);
        Integer reservasAtletismo = reservaRepository.obtenerNumeroReservasPorServicio(3);
        Double pagoPlinMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(3); // ID para Plin
        Double pagoYapeMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(2); // ID para Yape
        Double pagoIzipayMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(1); // ID para Izipay
        Double pagoEfectivoMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(4);
        Double pagoPlinSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(3); // ID para Plin
        Double pagoYapeSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(2); // ID para Yape
        Double pagoIzipaySemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(1); // ID para Izipay
        Double pagoEfectivoSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(4); // ID para Efectivo
        List<Integer> chartData = Arrays.asList(reservasGimnasio, reservasFutbol, reservasAtletismo, reservasPiscina);

        // Calcular la diferencia en las reservas y definir el badge
        String badge;
        long diferencia = numeroReservasMes - numeroReservasMesPasado;
        if (diferencia < 0) {
            badge = "bg-success-subtle text-danger";
        } else {
            badge = "bg-success-subtle text-success";
        }

        // Agregar los datos al modelo
        model.addAttribute("numeroReservasMes", numeroReservasMes);
        model.addAttribute("diferencia", diferencia);
        model.addAttribute("badge", badge);
        model.addAttribute("montoMensual", montoMensual);
        model.addAttribute("promedioMensual", promedioMensual);
        model.addAttribute("montoMensualPasado", montoMensualPasado);
        model.addAttribute("avisos", avisos);
        model.addAttribute("ultimoAviso", ultimoAviso);
        model.addAttribute("chartData", chartData);
        model.addAttribute("pagoPlinMensual", pagoPlinMensual);
        model.addAttribute("pagoYapeMensual", pagoYapeMensual);
        model.addAttribute("pagoIzipayMensual", pagoIzipayMensual);
        model.addAttribute("pagoEfectivoMensual", pagoEfectivoMensual);
        model.addAttribute("pagoPlinSemanal", pagoPlinSemanal);
        model.addAttribute("pagoYapeSemanal", pagoYapeSemanal);
        model.addAttribute("pagoIzipaySemanal", pagoIzipaySemanal);
        model.addAttribute("pagoEfectivoSemanal", pagoEfectivoSemanal);

        // Retornar la vista HTML
        return "Superadmin/Dashboard";  // Nombre de la vista (dashboard.html)
    }


    @GetMapping("/avisos")
    public String listarAvisos() {
        return "Superadmin/verAvisos";
    }

    @GetMapping("/reservas")
    public String listarReservas() {
        return "Superadmin/Reservas";
    }

    @GetMapping("/transacciones")
    public String listarTransacciones() {
        return "Superadmin/Transacciones";
    }

    @GetMapping("/reserva")
    public String verReserva() {
        return "Superadmin/VerReserva";
    }

    @GetMapping("/verPagosReservas")
    public String listarPagosReservasPerfil() {
        return "Superadmin/verPagosReservasPerfil";
    }

    @GetMapping("/perfil")
    public String verPerfil() {
        return "Superadmin/verPerfil";
    }
}
