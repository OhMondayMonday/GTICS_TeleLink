package com.example.telelink.service;

import com.example.telelink.entity.Mensaje;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ConversacionService conversacionService;
    private final ContactoService contactoService;
    private final WebClient webClient = WebClient.create("https://generativelanguage.googleapis.com");

    public GeminiService(ConversacionService conversacionService, ContactoService contactoService) {
        this.conversacionService = conversacionService;
        this.contactoService = contactoService;
    }

    public String responderConContexto(String pregunta, Integer usuarioId) {
        logger.info("Procesando pregunta con contexto: {} para usuario: {}", pregunta, usuarioId);

        try {
            // Verificar consultas específicas primero
            String respuestaEspecifica = verificarConsultasEspecificas(pregunta);
            if (respuestaEspecifica != null) {
                return respuestaEspecifica;
            }

            // Obtener contexto de conversación
            List<Mensaje> historial = conversacionService.obtenerUltimosMensajes(usuarioId, 10);
            String contextoConversacion = construirContextoConversacion(historial);
            String promptCompleto = crearPromptConContextoMunicipal(contextoConversacion, pregunta);

            return llamarGeminiAPI(promptCompleto);

        } catch (Exception e) {
            logger.error("Error en responderConContexto", e);
            return "Disculpa, hubo un error. Intenta nuevamente o llámanos al " + contactoService.obtenerTelefono();
        }
    }

    public String responder(String pregunta) {
        logger.info("Procesando pregunta sin contexto: {}", pregunta);

        try {
            String respuestaEspecifica = verificarConsultasEspecificas(pregunta);
            if (respuestaEspecifica != null) {
                return respuestaEspecifica;
            }

            String promptCompleto = crearPromptMunicipal(pregunta);
            return llamarGeminiAPI(promptCompleto);

        } catch (Exception e) {
            logger.error("Error en responder", e);
            return "Disculpa, hubo un error. Intenta nuevamente o llámanos al " + contactoService.obtenerTelefono();
        }
    }

    /**
     * Verificar consultas específicas con respuestas mejoradas
     */
    private String verificarConsultasEspecificas(String pregunta) {
        try {
            String preguntaLower = normalizarTexto(pregunta);
            logger.debug("Verificando consulta: '{}'", preguntaLower);

            // Horarios - respuestas más directas
            if (containsAnyKeyword(preguntaLower, new String[]{
                    "que hora abre", "a que hora abre", "cuando abre", "hora de apertura"
            })) {
                return "📅 Abrimos a las **8:00 AM** de lunes a viernes.";
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "que hora cierra", "a que hora cierra", "cuando cierra", "hora de cierre", "hasta que hora"
            })) {
                return "📅 Cerramos a las **5:00 PM** de lunes a viernes.";
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "horario", "horarios", "atencion", "funcionamiento"
            })) {
                return "🕒 **Horarios:** Lunes a Viernes de 8:00 AM a 5:00 PM\n📍 " + contactoService.obtenerDireccion();
            }

            // Canchas deportivas - información específica
            if (containsAnyKeyword(preguntaLower, new String[]{
                    "que canchas tienen", "canchas disponibles", "tipos de cancha"
            })) {
                return "🏟️ **Canchas disponibles:**\n" +
                        "⚽ Fútbol (grass sintético)\n" +
                        "🏐 Vóley (techada)\n" +
                        "🏀 Básquet (techada)\n" +
                        "🎾 Fulbito (sintético)\n\n" +
                        "Para reservar: " + contactoService.obtenerTelefono();
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "cancha de futbol", "canchas de futbol", "futbol", "grass"
            })) {
                return "⚽ **Cancha de Fútbol:**\n" +
                        "• Grass sintético\n" +
                        "• Medidas reglamentarias\n" +
                        "• Iluminación nocturna\n\n" +
                        "📞 Reservas: " + contactoService.obtenerTelefono();
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "cancha de voley", "canchas de voley", "volley", "vóley"
            })) {
                return "🏐 **Cancha de Vóley:**\n" +
                        "• Techada\n" +
                        "• Piso de parquet\n" +
                        "• Red reglamentaria\n\n" +
                        "📞 Reservas: " + contactoService.obtenerTelefono();
            }

            // Información de contacto básica
            if (containsAnyKeyword(preguntaLower, new String[]{
                    "telefono", "teléfono", "llamar", "numero"
            })) {
                return "📞 **Teléfonos:**\n" +
                        "☎️ Central: " + contactoService.obtenerTelefono() + "\n" +
                        "🚨 Emergencias: " + contactoService.obtenerTelefonoEmergencias();
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "direccion", "dirección", "ubicacion", "donde estan"
            })) {
                return "📍 **Ubicación:** " + contactoService.obtenerDireccion() + "\n" +
                        "🕒 Lunes a Viernes de 8:00 AM a 5:00 PM";
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "whatsapp", "whats", "wsp", "mensaje"
            })) {
                return "📱 **WhatsApp:** " + contactoService.obtenerWhatsApp();
            }

            if (containsAnyKeyword(preguntaLower, new String[]{
                    "pagina web", "página web", "sitio web", "web"
            })) {
                return "🌐 **Página Web:** " + contactoService.obtenerPaginaWeb();
            }

            // Reservas
            if (containsAnyKeyword(preguntaLower, new String[]{
                    "como reservar", "reservar cancha", "reserva", "como hago para reservar"
            })) {
                return "📋 **Para reservar una cancha:**\n" +
                        "1️⃣ Llama al " + contactoService.obtenerTelefono() + "\n" +
                        "2️⃣ Visita " + contactoService.obtenerPaginaWeb() + "\n" +
                        "3️⃣ O acércate a nuestras oficinas\n\n" +
                        "🕒 Atención: Lunes a Viernes 8:00 AM - 5:00 PM";
            }

            return null; // No es consulta específica

        } catch (Exception e) {
            logger.error("Error al verificar consultas específicas", e);
            return null;
        }
    }

    private String crearPromptMunicipal(String pregunta) {
        return "Eres SanMI Bot de la Municipalidad Distrital de San Miguel. " +
                "Responde de forma amigable, directa y útil. No repitas información innecesaria.\n\n" +

                "INFORMACIÓN MUNICIPAL:\n" +
                "- Dirección: " + contactoService.obtenerDireccion() + "\n" +
                "- Teléfono: " + contactoService.obtenerTelefono() + "\n" +
                "- WhatsApp: " + contactoService.obtenerWhatsApp() + "\n" +
                "- Web: " + contactoService.obtenerPaginaWeb() + "\n" +
                "- Horario: Lunes a Viernes 8:00 AM - 5:00 PM\n\n" +

                "CANCHAS DEPORTIVAS:\n" +
                "- Fútbol (grass sintético)\n" +
                "- Vóley (techada)\n" +
                "- Básquet (techada)\n" +
                "- Fulbito (sintético)\n\n" +

                "Vecino: " + pregunta;
    }

    private String crearPromptConContextoMunicipal(String contextoConversacion, String preguntaActual) {
        return "Eres SanMI Bot de la Municipalidad Distrital de San Miguel. " +
                "Responde considerando el contexto previo. Sé directo y útil.\n\n" +

                "INFORMACIÓN MUNICIPAL:\n" +
                "- Dirección: " + contactoService.obtenerDireccion() + "\n" +
                "- Teléfono: " + contactoService.obtenerTelefono() + "\n" +
                "- WhatsApp: " + contactoService.obtenerWhatsApp() + "\n" +
                "- Web: " + contactoService.obtenerPaginaWeb() + "\n" +
                "- Horario: Lunes a Viernes 8:00 AM - 5:00 PM\n\n" +

                "CANCHAS DEPORTIVAS:\n" +
                "- Fútbol (grass sintético)\n" +
                "- Vóley (techada)\n" +
                "- Básquet (techada)\n" +
                "- Fulbito (sintético)\n\n" +

                contextoConversacion + "\n" +
                "Vecino: " + preguntaActual;
    }

    private boolean containsAnyKeyword(String mensaje, String[] keywords) {
        for (String keyword : keywords) {
            if (mensaje.contains(normalizarTexto(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";

        return texto.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .trim();
    }

    private String construirContextoConversacion(List<Mensaje> historial) {
        if (historial == null || historial.isEmpty()) {
            return "Nueva conversación.";
        }

        StringBuilder contexto = new StringBuilder("Conversación previa:\n");
        for (Mensaje mensaje : historial) {
            String origen = mensaje.getOrigen() == Mensaje.Origen.usuario ? "Vecino" : "Bot";
            contexto.append(origen).append(": ").append(mensaje.getTextoMensaje()).append("\n");
        }
        return contexto.toString();
    }

    private String llamarGeminiAPI(String prompt) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return "Error de configuración. Contacta soporte técnico.";
            }

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            Map response = webClient.post()
                    .uri("/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extraerRespuestaDeAPI(response);

        } catch (WebClientResponseException e) {
            logger.error("Error WebClient: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Servicio temporalmente no disponible. Llama al " + contactoService.obtenerTelefono();
        } catch (Exception e) {
            logger.error("Error general Gemini API", e);
            return "Error de conexión. Intenta más tarde o llama al " + contactoService.obtenerTelefono();
        }
    }

    private String extraerRespuestaDeAPI(Map response) {
        try {
            if (response == null) return "Error: Respuesta vacía";

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String textoRespuesta = parts.get(0).get("text");
                        if (textoRespuesta != null && !textoRespuesta.trim().isEmpty()) {
                            return textoRespuesta;
                        }
                    }
                }
            }

            return "No se pudo generar respuesta";

        } catch (Exception e) {
            logger.error("Error al extraer respuesta", e);
            return "Error al procesar respuesta";
        }
    }
}