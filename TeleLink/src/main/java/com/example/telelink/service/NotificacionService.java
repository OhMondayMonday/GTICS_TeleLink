package com.example.telelink.service;

import com.example.telelink.entity.Notificacion;
import com.example.telelink.entity.TipoNotificacion;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.NotificacionRepository;
import com.example.telelink.repository.TipoNotificacionRepository;
import com.example.telelink.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private TipoNotificacionRepository tipoNotificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtiene las últimas 5 notificaciones para el dropdown
     */
    public List<Notificacion> obtenerUltimas5Notificaciones(Integer usuarioId) {
        return notificacionRepository.findTop5ByUsuario_UsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    /**
     * Cuenta las notificaciones no leídas
     */
    public long contarNotificacionesNoLeidas(Integer usuarioId) {
        return notificacionRepository.countByUsuario_UsuarioIdAndEstado(usuarioId, Notificacion.Estado.no_leido);
    }

    /**
     * Marca una notificación como leída
     */
    public void marcarComoLeida(Integer notificacionId) {
        Optional<Notificacion> notificacionOpt = notificacionRepository.findById(notificacionId);
        if (notificacionOpt.isPresent()) {
            Notificacion notificacion = notificacionOpt.get();
            notificacion.setEstado(Notificacion.Estado.leido);
            notificacionRepository.save(notificacion);
        }
    }

    /**
     * Marca todas las notificaciones como leídas para un usuario
     */
    public void marcarTodasComoLeidas(Integer usuarioId) {
        List<Notificacion> notificaciones = notificacionRepository.findTop5ByUsuario_UsuarioIdOrderByFechaCreacionDesc(usuarioId);
        for (Notificacion notificacion : notificaciones) {
            if (notificacion.getEstado() == Notificacion.Estado.no_leido) {
                notificacion.setEstado(Notificacion.Estado.leido);
            }
        }
        notificacionRepository.saveAll(notificaciones);
    }

    /**
     * Obtiene todas las notificaciones con filtros y paginación
     */
    public Page<Notificacion> obtenerNotificacionesConFiltros(
            Integer usuarioId,
            Notificacion.Estado estado,
            Integer tipoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {
        
        return notificacionRepository.findNotificacionesConFiltros(
            usuarioId, estado, tipoId, fechaInicio, fechaFin, pageable);
    }

    /**
     * Obtiene todas las notificaciones de un usuario
     */
    public Page<Notificacion> obtenerTodasLasNotificaciones(Integer usuarioId, Pageable pageable) {
        return notificacionRepository.findByUsuario_UsuarioIdOrderByFechaCreacionDesc(usuarioId, pageable);
    }

    /**
     * Crea una nueva notificación
     */
    public Notificacion crearNotificacion(Integer usuarioId, String tipoNotificacion, 
                                        String titulo, String mensaje, String urlRedireccion) {
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        Optional<TipoNotificacion> tipoOpt = tipoNotificacionRepository.findByTipoNotificacion(tipoNotificacion);
        
        if (usuarioOpt.isPresent() && tipoOpt.isPresent()) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuarioOpt.get());
            notificacion.setTipoNotificacion(tipoOpt.get());
            notificacion.setTituloNotificacion(titulo);
            notificacion.setMensaje(mensaje);
            notificacion.setUrlRedireccion(urlRedireccion);
            notificacion.setEstado(Notificacion.Estado.no_leido);
            
            return notificacionRepository.save(notificacion);
        }
        return null;
    }

    /**
     * Obtiene una notificación por ID
     */
    public Optional<Notificacion> obtenerNotificacionPorId(Integer notificacionId) {
        return notificacionRepository.findById(notificacionId);
    }

    /**
     * Obtiene todos los tipos de notificación
     */
    public List<TipoNotificacion> obtenerTodosLosTipos() {
        return tipoNotificacionRepository.findAll();
    }

    /**
     * Crea notificación para coordinadores cuando se les asigna una asistencia
     */
    public void crearNotificacionAsistenciaAsignada(Integer coordinadorId, String nombreActividad) {
        crearNotificacion(
            coordinadorId,
            "asignación",
            "Nueva asistencia asignada",
            "Se te ha asignado una nueva asistencia para la actividad: " + nombreActividad,
            "/coordinador/asistencias"
        );
    }

    /**
     * Crea notificación para coordinadores cuando hay una nueva reserva
     */
    public void crearNotificacionNuevaReserva(Integer coordinadorId, String nombreEspacio) {
        crearNotificacion(
            coordinadorId,
            "creación",
            "Nueva reserva",
            "Se ha realizado una nueva reserva en el espacio: " + nombreEspacio,
            "/coordinador/reservas"
        );
    }

    /**
     * Crea notificación para coordinadores cuando se cancela una reserva
     */
    public void crearNotificacionReservaCancelada(Integer coordinadorId, String nombreEspacio) {
        crearNotificacion(
            coordinadorId,
            "cancelación",
            "Reserva cancelada",
            "Se ha cancelado una reserva en el espacio: " + nombreEspacio,
            "/coordinador/reservas"
        );
    }

    /**
     * Crea notificación para coordinadores cuando se actualiza una actividad
     */
    public void crearNotificacionActividadActualizada(Integer coordinadorId, String nombreActividad) {
        crearNotificacion(
            coordinadorId,
            "actualización",
            "Actividad actualizada",
            "La actividad " + nombreActividad + " ha sido actualizada",
            "/coordinador/actividades"
        );
    }

    /**
     * Crea notificación para coordinadores cuando se aprueba algo
     */
    public void crearNotificacionAprobacion(Integer coordinadorId, String elemento) {
        crearNotificacion(
            coordinadorId,
            "aprobación",
            "Elemento aprobado",
            elemento + " ha sido aprobado",
            "/coordinador/dashboard"
        );
    }

    /**
     * Métodos específicos para SUPERADMIN
     */

    /**
     * Crea notificación para superadmin sobre nuevos registros de usuarios
     */
    public void crearNotificacionNuevoUsuario(Integer superadminId, String nombreUsuario, String rol) {
        crearNotificacion(
            superadminId,
            "creación",
            "Nuevo usuario registrado",
            "Se ha registrado un nuevo " + rol + ": " + nombreUsuario,
            "/superadmin/usuarios"
        );
    }

    /**
     * Crea notificación para superadmin sobre problemas del sistema
     */
    public void crearNotificacionProblema(Integer superadminId, String problema) {
        crearNotificacion(
            superadminId,
            "aviso",
            "Problema del sistema",
            problema,
            "/superadmin/dashboard"
        );
    }

    /**
     * Crea notificación para superadmin sobre transacciones importantes
     */
    public void crearNotificacionTransaccion(Integer superadminId, String tipoTransaccion, String monto) {
        crearNotificacion(
            superadminId,
            "actualización",
            tipoTransaccion + " procesado",
            "Se ha procesado un " + tipoTransaccion.toLowerCase() + " por " + monto,
            "/superadmin/transacciones"
        );
    }

    /**
     * Crea notificación para superadmin sobre cambios en establecimientos
     */
    public void crearNotificacionEstablecimiento(Integer superadminId, String accion, String nombreEstablecimiento) {
        crearNotificacion(
            superadminId,
            "actualización",
            "Establecimiento " + accion.toLowerCase(),
            "El establecimiento " + nombreEstablecimiento + " ha sido " + accion.toLowerCase(),
            "/superadmin/establecimientos"
        );
    }
}
