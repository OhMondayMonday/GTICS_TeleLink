package com.example.telelink.service;

import com.example.telelink.entity.Mensaje;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ConversacionService conversacionService;
    private final WebClient webClient = WebClient.create("https://generativelanguage.googleapis.com");

    public GeminiService(ConversacionService conversacionService) {
        this.conversacionService = conversacionService;
    }

    /**
     * Responder con contexto de conversación
     */
    public String responderConContexto(String pregunta, Integer usuarioId) {
        try {
            // Obtener últimos mensajes para contexto (últimos 10 mensajes)
            List<Mensaje> historial = conversacionService.obtenerUltimosMensajes(usuarioId, 10);

            // Construir el contexto de la conversación
            String contextoConversacion = construirContextoConversacion(historial);

            // Crear el prompt con contexto
            String promptCompleto = crearPromptConContexto(contextoConversacion, pregunta);

            // Llamar a Gemini API
            String respuesta = llamarGeminiAPI(promptCompleto);

            return respuesta;

        } catch (Exception e) {
            return "Error al conectarse con Gemini: " + e.getMessage();
        }
    }

    /**
     * Responder sin contexto (método original)
     */
    public String responder(String pregunta) {
        String modelo = "gemini-1.5-flash";
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text",
                                                "Eres SanMI Bot, un asistente virtual de la Municipalidad de San Miguel. " +
                                                        "Ayuda en español a los vecinos a reservar canchas, consultar horarios deportivos o contactar a la municipalidad. " +
                                                        "Responde brevemente y con amabilidad.\nVecino: " + pregunta)
                                )
                        )
                )
        );

        try {
            Map response = webClient.post()
                    .uri("/v1beta/models/" + modelo + ":generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extraerRespuestaDeAPI(response);

        } catch (WebClientResponseException e) {
            return "Error al conectarse con Gemini: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error al conectarse con Gemini: " + e.getMessage();
        }
    }

    /**
     * Construir contexto de conversación a partir del historial
     */
    private String construirContextoConversacion(List<Mensaje> historial) {
        if (historial.isEmpty()) {
            return "Esta es una nueva conversación.";
        }

        StringBuilder contexto = new StringBuilder();
        contexto.append("Historial de la conversación:\n");

        for (Mensaje mensaje : historial) {
            String origen = mensaje.getOrigen() == Mensaje.Origen.usuario ? "Vecino" : "SanMI Bot";
            contexto.append(origen).append(": ").append(mensaje.getTextoMensaje()).append("\n");
        }

        return contexto.toString();
    }

    /**
     * Crear prompt completo con contexto
     */
    private String crearPromptConContexto(String contextoConversacion, String preguntaActual) {
        return "Eres SanMI Bot, un asistente virtual de la Municipalidad de San Miguel. " +
                "Ayuda en español a los vecinos a reservar canchas, consultar horarios deportivos o contactar a la municipalidad. " +
                "Responde brevemente y con amabilidad. Ten en cuenta el contexto de la conversación previa.\n\n" +
                contextoConversacion + "\n" +
                "Vecino: " + preguntaActual;
    }

    /**
     * Llamar a Gemini API con el prompt
     */
    private String llamarGeminiAPI(String prompt) {
        String modelo = "gemini-1.5-flash";
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        Map response = webClient.post()
                .uri("/v1beta/models/" + modelo + ":generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return extraerRespuestaDeAPI(response);
    }

    /**
     * Extraer respuesta de la API response
     */
    private String extraerRespuestaDeAPI(Map response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content != null) {
                List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).get("text");
                }
            }
        }
        return "No se pudo obtener respuesta del modelo";
    }
}