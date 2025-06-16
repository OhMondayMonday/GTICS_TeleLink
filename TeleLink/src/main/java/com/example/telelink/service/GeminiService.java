package com.example.telelink.service;

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

    // ✅ URL base correcta para Gemini API
    private final WebClient webClient = WebClient.create("https://generativelanguage.googleapis.com");

    public String responder(String pregunta) {
        // ✅ Usar gemini-1.5-flash o gemini-1.5-pro (gemini-pro está deprecado)
        String modelo = "gemini-1.5-flash";

        // ✅ Estructura correcta del payload
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
            // ✅ URL correcta: /v1beta/models/{model}:generateContent
            Map response = webClient.post()
                    .uri("/v1beta/models/" + modelo + ":generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // ✅ Extraer la respuesta
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

        } catch (WebClientResponseException e) {
            // ✅ Manejo mejorado de errores
            return "Error al conectarse con Gemini: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error al conectarse con Gemini: " + e.getMessage();
        }
    }
}