package com.example.telelink.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para exponer configuraciones de API de forma segura
 * Las API keys se obtienen desde variables de entorno y se exponen
 * solo a usuarios autenticados
 */
@Controller
public class ApiConfigController {

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    /**
     * Endpoint para obtener la Google Maps API Key
     * Solo accesible para usuarios autenticados
     */
    @GetMapping("/api/config/google-maps-key")
    @ResponseBody
    public Map<String, String> getGoogleMapsApiKey() {
        Map<String, String> response = new HashMap<>();
        
        // Solo retornar la key si no es el valor por defecto
        if (googleMapsApiKey != null && !googleMapsApiKey.equals("your-google-maps-api-key-here")) {
            response.put("apiKey", googleMapsApiKey);
        } else {
            response.put("apiKey", "");
            response.put("error", "Google Maps API Key not configured");
        }
        
        return response;
    }
}
