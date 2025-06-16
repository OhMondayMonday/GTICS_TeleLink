package com.example.telelink.controller;

import com.example.telelink.service.GeminiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiService geminiService;

    public ChatbotController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping
    public Map<String, String> conversar(@RequestBody Map<String, Object> body) {
        String pregunta = (String) body.get("pregunta");
        String respuesta = geminiService.responder(pregunta);
        return Map.of("respuesta", respuesta);
    }
}



