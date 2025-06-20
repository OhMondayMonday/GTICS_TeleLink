package com.example.telelink.service;

import com.example.telelink.entity.Conversacion;
import com.example.telelink.entity.Mensaje;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.ConversacionRepository;
import com.example.telelink.repository.MensajeRepository;
import com.example.telelink.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConversacionService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    public ConversacionService(ConversacionRepository conversacionRepository,
                               MensajeRepository mensajeRepository,
                               UsuarioRepository usuarioRepository) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtener o crear una conversación activa para un usuario
     */
    public Conversacion obtenerOCrearConversacionActiva(Integer usuarioId) {
        // Buscar conversación activa existente
        Optional<Conversacion> conversacionActiva = conversacionRepository
                .findByUsuarioIdAndEstado(usuarioId, Conversacion.Estado.en_proceso);

        if (conversacionActiva.isPresent()) {
            return conversacionActiva.get();
        }

        // Si no existe, crear nueva conversación
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Conversacion nuevaConversacion = new Conversacion();
        nuevaConversacion.setUsuario(usuario);
        nuevaConversacion.setInicioConversacion(LocalDateTime.now());
        nuevaConversacion.setEstado(Conversacion.Estado.en_proceso);

        return conversacionRepository.save(nuevaConversacion);
    }

    /**
     * Guardar mensaje del usuario
     */
    public Mensaje guardarMensajeUsuario(Integer usuarioId, String textoMensaje) {
        Conversacion conversacion = obtenerOCrearConversacionActiva(usuarioId);

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setTextoMensaje(textoMensaje);
        mensaje.setOrigen(Mensaje.Origen.usuario);
        mensaje.setFecha(LocalDateTime.now());

        return mensajeRepository.save(mensaje);
    }

    /**
     * Guardar respuesta del chatbot
     */
    public Mensaje guardarMensajeChatbot(Integer usuarioId, String textoRespuesta) {
        Conversacion conversacion = obtenerOCrearConversacionActiva(usuarioId);

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setTextoMensaje(textoRespuesta);
        mensaje.setOrigen(Mensaje.Origen.chatbot);
        mensaje.setFecha(LocalDateTime.now());

        return mensajeRepository.save(mensaje);
    }

    /**
     * Obtener historial de mensajes de la conversación activa
     */
    public List<Mensaje> obtenerHistorialConversacion(Integer usuarioId) {
        Optional<Conversacion> conversacionActiva = conversacionRepository
                .findByUsuarioIdAndEstado(usuarioId, Conversacion.Estado.en_proceso);

        if (conversacionActiva.isPresent()) {
            return mensajeRepository.findByConversacionConversacionIdOrderByFechaAsc(
                    conversacionActiva.get().getConversacionId());
        }

        return new ArrayList<>();
    }

    /**
     * Obtener últimos N mensajes para contexto
     */
    public List<Mensaje> obtenerUltimosMensajes(Integer usuarioId, int limite) {
        Optional<Conversacion> conversacionActiva = conversacionRepository
                .findByUsuarioIdAndEstado(usuarioId, Conversacion.Estado.en_proceso);

        if (conversacionActiva.isPresent()) {
            Pageable pageable = PageRequest.of(0, limite);
            List<Mensaje> mensajes = mensajeRepository.findTopNByConversacionId(
                    conversacionActiva.get().getConversacionId(), pageable);

            // Invertir el orden para que aparezcan cronológicamente
            Collections.reverse(mensajes);
            return mensajes;
        }

        return new ArrayList<>();
    }

    /**
     * Finalizar conversación activa
     */
    public void finalizarConversacion(Integer usuarioId) {
        Optional<Conversacion> conversacionActiva = conversacionRepository
                .findByUsuarioIdAndEstado(usuarioId, Conversacion.Estado.en_proceso);

        if (conversacionActiva.isPresent()) {
            Conversacion conversacion = conversacionActiva.get();
            conversacion.setEstado(Conversacion.Estado.finalizada);
            conversacion.setFinConversacion(LocalDateTime.now());
            conversacionRepository.save(conversacion);
        }
    }

    /**
     * Obtener todas las conversaciones de un usuario
     */
    public List<Conversacion> obtenerConversacionesUsuario(Integer usuarioId) {
        return conversacionRepository.findByUsuarioUsuarioIdOrderByInicioConversacionDesc(usuarioId);
    }

    public void eliminarMensajesDeConversacion(Integer conversacionId) {
        List<Mensaje> mensajes = mensajeRepository.findByConversacionConversacionIdOrderByFechaAsc(conversacionId);
        mensajeRepository.deleteAll(mensajes);
    }

    // ConversacionService.java
    public Optional<Conversacion> obtenerConversacionActiva(Usuario usuario) {
        return conversacionRepository.findByUsuarioIdAndEstado(
                usuario.getUsuarioId(),
                Conversacion.Estado.en_proceso
        );
    }

    public Conversacion obtenerConversacionActivaOCrear(Usuario usuario) {
        return conversacionRepository.findByUsuarioIdAndEstado(usuario.getUsuarioId(), Conversacion.Estado.en_proceso)
                .orElseGet(() -> {
                    Conversacion nueva = new Conversacion();
                    nueva.setUsuario(usuario);
                    nueva.setInicioConversacion(LocalDateTime.now());
                    nueva.setEstado(Conversacion.Estado.en_proceso);
                    return conversacionRepository.save(nueva);
                });
    }

    /**
     * Obtener conversación activa por ID de usuario
     */
    public Optional<Conversacion> obtenerConversacionActivaPorUsuarioId(Integer usuarioId) {
        return conversacionRepository.findByUsuarioIdAndEstado(
                usuarioId,
                Conversacion.Estado.en_proceso
        );
    }
}