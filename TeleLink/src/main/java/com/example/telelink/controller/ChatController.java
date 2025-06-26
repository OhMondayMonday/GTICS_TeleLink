package com.example.telelink.controller;

import com.example.telelink.langchain4j.LangChain4jAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final LangChain4jAssistant assistant;

    public ChatController(LangChain4jAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        return assistant.chat(request.chatId(), request.message());
    }

    record ChatRequest(String chatId, String message) {}
}