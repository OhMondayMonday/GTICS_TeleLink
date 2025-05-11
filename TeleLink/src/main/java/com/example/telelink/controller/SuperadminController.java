package com.example.telelink.controller;

import com.example.telelink.dto.Superadmin.AvisoDTO;
import com.example.telelink.entity.*;

import com.example.telelink.repository.AvisoRepository;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private AvisoRepository avisoRepository;
    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/pagos")
    public String listarPagos(Model model) {
        List<Pago> pagos = pagoRepository.findAllWithRelations();
        model.addAttribute("pagos", pagos);
        return "Superadmin/verPagos";
    }

    @GetMapping("/inicio")
    public String dashboard(Model model) {

        // Obtener los datos de reservas y pagos
        Integer numeroReservasMes = reservaRepository.numeroReservasMes();
        Integer numeroReservasMesPasado = reservaRepository.numeroReservasMesPasado();
        Double montoMensual = reservaRepository.obtenerMontoTotalDeReservasEsteMes();
        montoMensual = montoMensual == null ? 0 : montoMensual;
        Double promedioMensual = montoMensual/numeroReservasMes;
        promedioMensual =  montoMensual == 0.0 ? 0.0 : promedioMensual;
        Double montoMensualPasado = reservaRepository.obtenerMontoTotalDeReservasMesPasado();
        montoMensualPasado = (montoMensualPasado == null) ? 0.0 : montoMensualPasado;
        double promedioMensualPasado = montoMensualPasado/numeroReservasMesPasado;
        promedioMensualPasado = (montoMensualPasado == 0.0) ? 0.0 : promedioMensualPasado;
        List<Aviso> avisos = avisoRepository.obtenerUltimosAvisos();
        Optional<Aviso> avisoOpt = avisoRepository.findAnyAvisoActivo();
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
            badge = "badge bg-danger-subtle text-danger font-size-11";
        } else {
            badge = "badge bg-success-subtle text-success font-size-11";
        }

        // Agregar los datos al modelo
        model.addAttribute("numeroReservasMes", numeroReservasMes);
        model.addAttribute("promedioMensualPasado", promedioMensualPasado);
        model.addAttribute("diferencia", diferencia);
        model.addAttribute("badge", badge);
        model.addAttribute("montoMensual", montoMensual);
        model.addAttribute("promedioMensual", promedioMensual);
        model.addAttribute("montoMensualPasado", montoMensualPasado);
        model.addAttribute("avisos", avisos);
        model.addAttribute("ultimoAviso", avisoOpt);
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

    @GetMapping("/editarAviso/{id}")
    public String mostrarFormularioEdicion(@PathVariable Integer id, Model model) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aviso no encontrado con ID: " + id));
        model.addAttribute("aviso", aviso);
        return "Superadmin/editarAviso";
    }

    @PostMapping("/actualizarAviso/{id}")
    public String actualizarAviso(
            @PathVariable Integer id,
            @ModelAttribute("aviso") Aviso avisoActualizado,
            RedirectAttributes redirectAttributes
    ) {
        Aviso avisoExistente = avisoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aviso no encontrado con ID: " + id));

        // Actualizar campos
        avisoExistente.setTituloAviso(avisoActualizado.getTituloAviso());
        avisoExistente.setTextoAviso(avisoActualizado.getTextoAviso());
        avisoExistente.setFotoAvisoUrl(avisoActualizado.getFotoAvisoUrl());

        avisoRepository.save(avisoExistente); // Guardar cambios

        redirectAttributes.addFlashAttribute("exito", "Aviso actualizado correctamente");
        return "redirect:/superadmin/avisos"; // Redirigir a la lista de avisos
    }



    @GetMapping("/avisos")
    public String listarAvisos(Model model) {
        // Obtener todos los avisos ordenados por fecha descendente
        List<Aviso> avisos = avisoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaAviso"));

        // Obtener el último aviso (el más reciente)
        Aviso ultimoAviso = avisos.isEmpty() ? null : avisos.get(0);

        // Agregar atributos al modelo
        model.addAttribute("avisos", avisos);
        model.addAttribute("ultimoAviso", ultimoAviso);

        return "Superadmin/verAvisos"; // Nombre de tu plantilla Thymeleaf
    }

    @GetMapping("/crearAviso")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("aviso", new Aviso());
        model.addAttribute("modo", "crear");
        return "Superadmin/crearAviso"; // Mismo formulario que para editar
    }

    // Procesar creación de aviso
    @PostMapping("/guardarAviso")
    public String guardarAviso(
            @ModelAttribute("aviso") @Valid Aviso nuevoAviso,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "Superadmin/crearAviso";
        }

        // Establecer fecha actual si no viene definida
        if (nuevoAviso.getFechaAviso() == null) {
            nuevoAviso.setFechaAviso(LocalDateTime.now());
        }

        // Establecer estado por defecto si no viene definido
        if (nuevoAviso.getEstadoAviso() == null || nuevoAviso.getEstadoAviso().isEmpty()) {
            nuevoAviso.setEstadoAviso("disponible");
        }

        avisoRepository.save(nuevoAviso);

        redirectAttributes.addFlashAttribute("exito", "Aviso creado correctamente");
        return "redirect:/superadmin/avisos";
    }

    @GetMapping("/reservas")
    public String listarReservas(Model model) {
        List<Reserva> reservas = reservaRepository.findAll();
        model.addAttribute("reservas", reservas);
        return "Superadmin/Reservas"; // Ruta de tu plantilla Thymeleaf
    }

    @GetMapping("reservas/{id}")
    public String verDetalleReserva(@PathVariable Integer id, Model model) {
        Reserva reserva = reservaRepository.findByReservaId(id);
        if (reserva == null) {
            // Manejar el caso cuando la reserva no existe
            return "redirect:/superadmin/reservas";
        }
        model.addAttribute("reserva", reserva);
        return "Superadmin/VerReserva"; // Plantilla para ver detalles
    }


    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        // Obtener estadísticas directamente desde el repositorio
        long totalUsuarios = usuarioRepository.count();
        long usuariosBaneados = usuarioRepository.countByEstadoCuenta(Usuario.EstadoCuenta.baneado);
        long nuevosUsuariosEsteMes = usuarioRepository.countUsuariosEsteMes();

        // Agregar datos al modelo
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosBaneados", usuariosBaneados);
        model.addAttribute("nuevosUsuariosEsteMes", nuevosUsuariosEsteMes);

        // Obtener lista completa de usuarios
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);

        return "Superadmin/Usuarios";
    }

    @GetMapping("usuarios/{id}")
    public String verDetalleUsuario(@PathVariable Integer id, Model model) {
        // Obtener el usuario por ID
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return "redirect:/superadmin/usuarios";
        }

        long reservasTotales = reservaRepository.countByUsuario(usuario);
        long reservasEsteMes = reservaRepository.countByUsuarioThisMonth(usuario);
        long reservasEstaSemana = reservaRepository.countByUsuarioThisWeek(usuario);

        // Obtener las últimas 6 reservas del usuario
        List<Reserva> ultimasReservas = reservaRepository.findTop6ByUsuarioOrderByInicioReservaDesc(usuario);

        // Agregar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("ultimasReservas", ultimasReservas);
        model.addAttribute("reservasTotales", reservasTotales);
        model.addAttribute("reservasEsteMes", reservasEsteMes);
        model.addAttribute("reservasEstaSemana", reservasEstaSemana);

        return "Superadmin/verPerfil";
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

}
