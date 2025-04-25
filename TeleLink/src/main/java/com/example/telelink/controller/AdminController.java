package com.example.telelink.controller;

import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EstablecimientoDeportivoRepository establecimientoDeportivoRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private ServicioDeportivoRepository servicioDeportivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ObservacionRepository observacionRepository;


    @GetMapping("establecimientos")
    public String listarEstablecimientos(Model model) {
        List<EstablecimientoDeportivo> establecimientosList = establecimientoDeportivoRepository.findAll();

        model.addAttribute("establecimientos", establecimientosList);
        return "admin/establecimientoList";
    }

    @GetMapping("establecimientos/nuevo")
    public String crearEstablecimiento(Model model) {
        model.addAttribute("establecimiento", new EstablecimientoDeportivo());
        return "admin/establecimientoForm";
    }

    @PostMapping("establecimientos/guardar")
    public String guardarEstablecimiento(EstablecimientoDeportivo establecimiento) {
        // Lógica para guardar el establecimiento en la base de datos
        establecimientoDeportivoRepository.save(establecimiento);
        return "redirect:/admin/establecimientos"; // Redirige a la lista de establecimientos
    }

    @GetMapping("establecimientos/info")
    public String infoEstablecimiento(@RequestParam("id") Integer id, Model model) {
        EstablecimientoDeportivo establecimientoDeportivo = establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id);
        model.addAttribute("establecimiento", establecimientoDeportivo);
        return "admin/establecimientoInfo";
    }

    @GetMapping("establecimientos/editar")
    public String editarEstablecimiento(@RequestParam("id") Integer id, Model model) {
        EstablecimientoDeportivo establecimientoDeportivo = establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id);
        model.addAttribute("establecimiento", establecimientoDeportivo);
        return "admin/establecimientoEditForm";
    }




    @GetMapping("espacios/nuevo")
    public String crearEspacioDeportivo(Model model) {
        model.addAttribute("espacioDeportivo", new EspacioDeportivo());
        model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
        model.addAttribute("servicios", servicioDeportivoRepository.findAll());
        return "admin/espacioForm";
    }

    @PostMapping("espacios/guardar")
    public String guardarEspacioDeportivo(EspacioDeportivo espacioDeportivo) {
        // Aquí no es necesario hacer los @RequestParam, ya que Thymeleaf vincula los campos al objeto espacioDeportivo.
        espacioDeportivo.setFechaCreacion(LocalDateTime.now());
        espacioDeportivo.setFechaActualizacion(LocalDateTime.now());

        // Guardar el espacio en la base de datos
        espacioDeportivoRepository.save(espacioDeportivo);

        return "redirect:/admin/establecimientos"; // Redirigir al listado de espacios
    }




    @GetMapping("coordinadores")
    public String listarCoordinadores(Model model) {
        List<Usuario> usuariosList = usuarioRepository.findAllByRol_Rol("coordinador");

        model.addAttribute("coodinadores", usuariosList);
        return "admin/coordinadorList";
    }

    @GetMapping("perfil")
    public String perfilAdministrador(Model model) {


        // GIan: Hard rocked btw
        Optional<Usuario> usuariosOptional = usuarioRepository.findById(10);
        if (usuariosOptional.isPresent()) {
            model.addAttribute("usuario", usuariosOptional.get());
            return "admin/adminPerfil";
        }
        else {
            return "redirect:/admin/establecimientos";
        }


    }






    @GetMapping("pagos")
    public String listarPagos(Model model) {
        List<Pago> pagosPendientes = pagoRepository.findByEstadoTransaccionAndMetodoPago_MetodoPago(
                Pago.EstadoTransaccion.pendiente, "Transaccion"
        );

        model.addAttribute("pagosPendientes", pagosPendientes);
        return "admin/pagosList";
    }

    @GetMapping("/pagos/aceptar")
    public String aceptarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.completado);
            pagoRepository.save(pago);
        }
        return "redirect:/admin/pagos";
    }

    @GetMapping("/pagos/rechazar")
    public String rechazarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.fallido);
            pagoRepository.save(pago);
        }
        return "redirect:/admin/pagos";
    }

    @GetMapping("/pagos/ver")
    public String verDetallePago(@RequestParam("id") Integer id, Model model) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            model.addAttribute("pago", optPago.get());
            return "admin/pagosInfo";
        }
        return "redirect:/admin/pagos/pendientes/transaccion";
    }



    @GetMapping("/observaciones")
    public String listarObservaciones(
            @RequestParam(required = false) String nivel,
            Model model) {

        List<Observacion> observaciones;

        if (nivel == null || nivel.equals("sin_filtro")) {
            // Obtener todas las observaciones sin filtro
            observaciones = observacionRepository.findAll();
        } else {
            try {
                Observacion.NivelUrgencia urgencia = Observacion.NivelUrgencia.valueOf(nivel);
                // Obtener observaciones filtradas por nivel de urgencia con todas las relaciones
                observaciones = observacionRepository.findAllByNivelUrgenciaWithRelationsNative(urgencia);
            } catch (IllegalArgumentException e) {
                observaciones = observacionRepository.findAll(); // fallback si hay error
            }
        }

        model.addAttribute("observaciones", observaciones);
        model.addAttribute("nivelSeleccionado", nivel == null ? "sin_filtro" : nivel);
        return "admin/observacionesList"; // tu HTML con la tabla
    }


    @GetMapping("dashboard")
    public String estadisticas(Model model) {

        model.addAttribute("reservasPorDia", usuarioRepository.obtenerCantidadReservasPorDia());

        return "admin/dashboard";

    }


}
