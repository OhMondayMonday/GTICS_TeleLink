package com.example.telelink.service;

import com.example.telelink.model.DniResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DniService {

    private final WebClient webClient;

    @Value("${apiperu.token}")
    private String bearerToken;

    public DniService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://apiperu.dev/api").build();
    }

    public Mono<DniResponse> consultarDni(String dni) {
        return webClient.post()
                .uri("/dni")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .bodyValue("{\"dni\": \"" + dni + "\"}")
                .retrieve()
                .bodyToMono(DniResponse.class);
    }

    public String formatName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }
}