package com.example.telelink.controller;

import com.example.telelink.dto.admin.CantidadReservasPorDiaDto;
import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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







    /*@GetMapping("perfil")
    public String perfilAdministrador(Model model, HttpServletRequest session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        // GIan: Hard rocked btw
        Optional<Usuario> usuariosOptional = usuarioRepository.findById();
        if (usuariosOptional.isPresent()) {
            model.addAttribute("usuario", usuariosOptional.get());
            return "admin/adminPerfil";
        }
        else {
            return "redirect:/openLoginWindow";
        }

    }*/

    /* Opcion 1:
    @GetMapping("/perfil")
    public String perfilAdministrador(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario != null) {
            Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuario.getUsuarioId());
            if (usuarioOptional.isPresent()) {
                model.addAttribute("usuario", usuarioOptional.get());
                return "admin/adminPerfil";
            }
        }

        return "redirect:/admin/dashboard";
    }
     */

    @GetMapping("/perfil")
    public String perfilAdministrador(@SessionAttribute("usuario") Usuario usuario, Model model) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuario.getUsuarioId());

        if (usuarioOptional.isPresent()) {
            //model.addAttribute("usuario", usuarioOptional.get());
            return "admin/adminPerfil";
        }

        return "redirect:/admin/dashboard";
    }




    @GetMapping("pagos")
    public String listarPagos(Model model) {

        List<Pago> pagosPendientes = pagoRepository.findByEstadoTransaccionAndMetodoPago_MetodoPagoId(
                Pago.EstadoTransaccion.pendiente, 1
        );


        //List<Pago> pagosPendientes = pagoRepository.findAll();

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

        List<Observacion.Estado> estadosVisibles = List.of(Observacion.Estado.pendiente, Observacion.Estado.en_proceso);
        List<Observacion> observaciones;

        if (nivel == null || nivel.equals("sin_filtro")) {
            // Obtener todas las observaciones con estado pendiente o en_proceso
            observaciones = observacionRepository.findByEstadoInOrderByEstadoAsc(estadosVisibles);
        } else {
            try {
                Observacion.NivelUrgencia urgencia = Observacion.NivelUrgencia.valueOf(nivel);
                observaciones = observacionRepository.findByEstadoInAndNivelUrgenciaOrderByEstadoAsc(estadosVisibles, urgencia);
            } catch (IllegalArgumentException e) {
                observaciones = observacionRepository.findByEstadoInOrderByEstadoAsc(estadosVisibles); // fallback si hay error
            }
        }

        model.addAttribute("observaciones", observaciones);
        model.addAttribute("nivelSeleccionado", nivel == null ? "sin_filtro" : nivel);
        return "admin/observacionesList";
    }

    @GetMapping("/observaciones/info")
    public String verInfoObservacion(@RequestParam("id") Integer id, Model model) {
        Observacion observacion = observacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID inválido"));

        // Si está en estado 'pendiente', lo cambiamos a 'en_proceso'
        if (observacion.getEstado() == Observacion.Estado.pendiente) {
            observacion.setEstado(Observacion.Estado.en_proceso);
            observacion.setFechaActualizacion(LocalDateTime.now());
            observacionRepository.save(observacion);
        }

        model.addAttribute("observacion", observacion);
        return "admin/observacionInfo";
    }

    @PostMapping("/observaciones/resolver")
    public String resolverObservacion(@RequestParam("id") Integer id,
                                      @RequestParam("comentarioAdministrador") String comentario) {
        Observacion observacion = observacionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID inválido"));
        observacion.setEstado(Observacion.Estado.resuelto);
        observacion.setComentarioAdministrador(comentario);
        observacion.setFechaActualizacion(LocalDateTime.now());
        observacionRepository.save(observacion);
        return "redirect:/admin/observaciones";
    }


    /*@GetMapping("/observaciones")
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

     */


    @GetMapping("/dashboard")
    public String estadisticas(Model model) {
        List<CantidadReservasPorDiaDto> reservasPorDia = usuarioRepository.obtenerCantidadReservasPorDia();

        // Calcular el total de reservas
        int totalReservas = reservasPorDia.stream()
                .mapToInt(CantidadReservasPorDiaDto::getCantidadReservas)
                .sum();

        // Calcular porcentajes y preparar datos para el gráfico
        List<Integer> chartData = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();
        List<Object[]> topDias = new ArrayList<>();

        for (CantidadReservasPorDiaDto reserva : reservasPorDia) {
            chartData.add(reserva.getCantidadReservas());
            chartLabels.add(reserva.getDia());
            double porcentaje = (reserva.getCantidadReservas() * 100.0) / totalReservas;
            topDias.add(new Object[]{reserva.getDia(), String.format("%.1f%%", porcentaje)});
        }

        // Seleccionar los 3 días con más reservas
        topDias.sort((a, b) -> Double.compare(
                Double.parseDouble(((String) b[1]).replace("%", "")),
                Double.parseDouble(((String) a[1]).replace("%", ""))
        ));
        List<Object[]> top3Dias = topDias.size() > 3 ? topDias.subList(0, 3) : topDias;

        model.addAttribute("reservasPorDia", reservasPorDia);
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("top3Dias", top3Dias);

        return "admin/dashboard";
    }

    /*
    @GetMapping("dashboard")
    public String estadisticas(Model model) {
        // Obtener reservas por día
        List<CantidadReservasPorDiaDto> reservasPorDia = usuarioRepository.obtenerCantidadReservasPorDia();
        System.out.println("reservasPorDia: " + reservasPorDia);

        // Calcular el total de reservas
        int totalReservas = reservasPorDia.stream()
                .mapToInt(CantidadReservasPorDiaDto::getCantidadReservas)
                .sum();
        System.out.println("totalReservas: " + totalReservas);

        // Preparar datos para el gráfico
        List<Integer> chartData = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();
        List<Object[]> topDias = new ArrayList<>();

        // Asegurar que los 7 días estén presentes
        String[] diasSemana = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        for (String dia : diasSemana) {
            boolean diaEncontrado = false;
            for (CantidadReservasPorDiaDto reserva : reservasPorDia) {
                if (reserva.getDia().equals(dia)) {
                    chartData.add(reserva.getCantidadReservas());
                    chartLabels.add(reserva.getDia());
                    double porcentaje = totalReservas > 0 ? (reserva.getCantidadReservas() * 100.0) / totalReservas : 0.0;
                    topDias.add(new Object[]{reserva.getDia(), String.format("%.1f%%", porcentaje)});
                    diaEncontrado = true;
                    break;
                }
            }
            if (!diaEncontrado) {
                chartData.add(0);
                chartLabels.add(dia);
                topDias.add(new Object[]{dia, "0.0%"});
            }
        }

        System.out.println("chartData: " + chartData);
        System.out.println("chartLabels: " + chartLabels);
        System.out.println("topDias: " + topDias);

        // Seleccionar los 3 días con más reservas
        List<Object[]> top3Dias = topDias.stream()
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(((String) b[1]).replace("%", "")),
                        Double.parseDouble(((String) a[1]).replace("%", ""))
                ))
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("top3Dias: " + top3Dias);

        // Añadir atributos al modelo
        model.addAttribute("reservasPorDia", reservasPorDia);
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("top3Dias", top3Dias);

        return "admin/dashboard";
    }
     */


}
