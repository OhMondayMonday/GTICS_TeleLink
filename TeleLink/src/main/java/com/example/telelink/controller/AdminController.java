package com.example.telelink.controller;

import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Modificacion
    @GetMapping("establecimientos/nuevo")
    public String crearEstablecimiento(@ModelAttribute("establecimiento") EstablecimientoDeportivo establecimiento, Model model) {
        return "admin/establecimientoForm";
    }


    @GetMapping("establecimientos/info")
    public String infoEstablecimiento(@ModelAttribute("establecimiento") EstablecimientoDeportivo establecimiento, @RequestParam("id") Integer id, Model model) {
        establecimiento = establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id);
        model.addAttribute("establecimiento", establecimiento);
        return "admin/establecimientoInfo";
    }


    // Recepciona un establecimiento con una id en específico
    @GetMapping("establecimientos/editar")
    public String editarEstablecimiento(@ModelAttribute("establecimiento") EstablecimientoDeportivo establecimiento, @RequestParam("id") Integer id, Model model) {

        Optional<EstablecimientoDeportivo> optEstablecimiento = Optional.ofNullable(establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id));

        if(optEstablecimiento.isPresent()) {

            establecimiento = optEstablecimiento.get();
            model.addAttribute("establecimiento", establecimiento);
            return "admin/establecimientoForm";

        }

        else {
            return "redirect:/admin/establecimientos";
        }
    }

    @PostMapping("establecimientos/guardar")
    public String guardarEstablecimiento(@ModelAttribute("establecimiento") @Valid EstablecimientoDeportivo establecimiento, BindingResult bindingResult, RedirectAttributes attr) {
        // Lógica para guardar el establecimiento en la base de datos

        if (bindingResult.hasErrors()) {
            return "admin/establecimientoForm";

        }

        else {
            if (establecimiento.getEstablecimientoDeportivoId() == null || establecimiento.getEstablecimientoDeportivoId() == 0) {
                attr.addFlashAttribute("msg", "Establecimiento creado satisfactoriamente Owo");
            }

            else {
                attr.addFlashAttribute("msg", "Establecimiento editado satisfactoriamente :D");
            }
            establecimientoDeportivoRepository.save(establecimiento);
            return "redirect:/admin/establecimientos"; // Redirige a la lista de establecimientos
        }

    }


    // Sección: Espacios

    @GetMapping("espacios")
    public String listarEspacios(Model model) {
        List<EspacioDeportivo> espaciosList = espacioDeportivoRepository.findAll();

        model.addAttribute("espacios", espaciosList);
        return "admin/espacioList";
    }

    @GetMapping("espacios/nuevo")
    public String crearEspacioDeportivo(@ModelAttribute("espacio") EspacioDeportivo espacio, Model model) {

        model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
        model.addAttribute("servicios", servicioDeportivoRepository.findAll());
        return "admin/espacioForm";
    }

    @PostMapping("espacios/guardar")
    public String guardarEspacioDeportivo(@ModelAttribute("espacio") @Valid EspacioDeportivo espacio, BindingResult bindingResult, Model model, RedirectAttributes attr) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            model.addAttribute("servicios", servicioDeportivoRepository.findAll());
            return "admin/espacioForm";

        }

        else {
            if (espacio.getEspacioDeportivoId() == null || espacio.getEspacioDeportivoId() == 0) {
                attr.addFlashAttribute("msg", "Espacio creado satisfactoriamente Owo");
            }

            else {
                attr.addFlashAttribute("msg", "Establecimiento ???? satisfactoriamente :D");
            }
            // Aquí no es necesario hacer los @RequestParam, ya que Thymeleaf vincula los campos al objeto espacioDeportivo.
            espacio.setFechaCreacion(LocalDateTime.now());
            espacio.setFechaActualizacion(LocalDateTime.now());
            espacioDeportivoRepository.save(espacio);
            return "redirect:/admin/establecimientos"; // Redirige a la lista de establecimientos
        }

    }


    // Sección: Coordinadores

    @GetMapping("coordinadores")
    public String listarCoordinadores(Model model) {
        List<Usuario> usuariosList = usuarioRepository.findAllByRol_Rol("coordinador");

        model.addAttribute("coordinadores", usuariosList);
        return "admin/coordinadorList";
    }



    @GetMapping("coordinadores/calendario")
    public String calendarioCoordinadores(@RequestParam Integer id, Model model) {

        Usuario coordinador = usuarioRepository.findByUsuarioId(id);

        model.addAttribute("coordinador", coordinador);

        return "admin/coordinadorCalendario";
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
