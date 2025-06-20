package com.example.telelink.controller;

import com.example.telelink.entity.Conversacion;
import com.example.telelink.entity.Mensaje;
import com.example.telelink.service.ConversacionService;
import com.example.telelink.service.GeminiService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiService geminiService;
    private final ConversacionService conversacionService;

    public ChatbotController(GeminiService geminiService, ConversacionService conversacionService) {
        this.geminiService = geminiService;
        this.conversacionService = conversacionService;
    }

    /**
     * Endpoint principal para conversar (con persistencia)
     */
    @PostMapping
    public Map<String, Object> conversar(@RequestBody Map<String, Object> body) {
        try {
            String pregunta = (String) body.get("pregunta");
            Integer usuarioId = (Integer) body.get("usuarioId");

            if (pregunta == null || pregunta.trim().isEmpty()) {
                return Map.of(
                        "error", true,
                        "mensaje", "La pregunta no puede estar vacía"
                );
            }

            if (usuarioId == null) {
                return Map.of(
                        "error", true,
                        "mensaje", "ID de usuario requerido"
                );
            }

            // 1. Guardar mensaje del usuario
            conversacionService.guardarMensajeUsuario(usuarioId, pregunta);

            // 2. Obtener respuesta del chatbot con contexto
            String respuesta = geminiService.responderConContexto(pregunta, usuarioId);

            // 3. Guardar respuesta del chatbot
            conversacionService.guardarMensajeChatbot(usuarioId, respuesta);

            return Map.of(
                    "respuesta", respuesta,
                    "error", false,
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            return Map.of(
                    "error", true,
                    "mensaje", "Error interno del servidor: " + e.getMessage()
            );
        }
    }

    /**
     * Obtener historial de conversación
     */
    @GetMapping("/historial/{usuarioId}")
    public Map<String, Object> obtenerHistorial(@PathVariable Integer usuarioId) {
        try {
            List<Mensaje> mensajes = conversacionService.obtenerHistorialConversacion(usuarioId);

            // SOLUCIÓN 1: Casting explícito de la lista
            List<Map<String, Object>> historial = (List<Map<String, Object>>) (List<?>) mensajes.stream()
                    .map(mensaje -> Map.<String, Object>of(
                            "id", mensaje.getMensajeId(),
                            "texto", mensaje.getTextoMensaje(),
                            "origen", mensaje.getOrigen().toString(),
                            "fecha", mensaje.getFecha().toString()
                    ))
                    .collect(Collectors.toList());

            return Map.of(
                    "historial", historial,
                    "error", false
            );

        } catch (Exception e) {
            return Map.of(
                    "error", true,
                    "mensaje", "Error al obtener historial: " + e.getMessage()
            );
        }
    }

    /**
     * Finalizar conversación actual
     */
    @PostMapping("/finalizar/{usuarioId}")
    public Map<String, Object> finalizarConversacion(@PathVariable Integer usuarioId) {
        try {
            conversacionService.finalizarConversacion(usuarioId);

            return Map.of(
                    "mensaje", "Conversación finalizada exitosamente",
                    "error", false
            );

        } catch (Exception e) {
            return Map.of(
                    "error", true,
                    "mensaje", "Error al finalizar conversación: " + e.getMessage()
            );
        }
    }

    /**
     * Obtener todas las conversaciones de un usuario
     */
    @GetMapping("/conversaciones/{usuarioId}")
    public Map<String, Object> obtenerConversaciones(@PathVariable Integer usuarioId) {
        try {
            List<Conversacion> conversaciones = conversacionService.obtenerConversacionesUsuario(usuarioId);

            // SOLUCIÓN 2: Usando HashMap en lugar de Map.of()
            List<Map<String, Object>> conversacionesData = conversaciones.stream()
                    .map(conv -> {
                        Map<String, Object> convMap = new HashMap<>();
                        convMap.put("id", conv.getConversacionId());
                        convMap.put("inicio", conv.getInicioConversacion().toString());
                        convMap.put("fin", conv.getFinConversacion() != null ? conv.getFinConversacion().toString() : null);
                        convMap.put("estado", conv.getEstado().toString());
                        return convMap;
                    })
                    .collect(Collectors.toList());

            return Map.of(
                    "conversaciones", conversacionesData,
                    "error", false
            );

        } catch (Exception e) {
            return Map.of(
                    "error", true,
                    "mensaje", "Error al obtener conversaciones: " + e.getMessage()
            );
        }
    }

    /**
     * Endpoint para respuestas rápidas (botones)
     */
    @PostMapping("/respuesta-rapida")
    public Map<String, Object> respuestaRapida(@RequestBody Map<String, Object> body) {
        String opcion = (String) body.get("opcion");
        Integer usuarioId = (Integer) body.get("usuarioId");

        // Mapear opciones a preguntas más naturales
        String pregunta = switch (opcion) {
            case "reservar" -> "¿Cómo puedo reservar una cancha deportiva?";
            case "horarios" -> "¿Cuáles son los horarios disponibles para las canchas?";
            case "contacto" -> "¿Cómo puedo contactar a la municipalidad?";
            default -> opcion;
        };

        // Usar el mismo flujo que el endpoint principal
        return conversar(Map.of(
                "pregunta", pregunta,
                "usuarioId", usuarioId
        ));
    }

    @DeleteMapping("/historial/{usuarioId}")
    public Map<String, Object> eliminarHistorial(@PathVariable Integer usuarioId) {
        try {
            Optional<Conversacion> conversacionActiva = conversacionService
                    .obtenerConversacionActivaPorUsuarioId(usuarioId);

            if (conversacionActiva.isPresent()) {
                Conversacion conversacion = conversacionActiva.get();
                conversacionService.eliminarMensajesDeConversacion(conversacion.getConversacionId());
            }

            return Map.of("mensaje", "Historial eliminado", "error", false);

        } catch (Exception e) {
            return Map.of(
                    "error", true,
                    "mensaje", "Error al eliminar historial: " + e.getMessage()
            );
        }
    }


}