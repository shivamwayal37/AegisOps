package io.aegisops.agent.analysis;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.aegisops.agent.incident.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmAnalyzer {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${aegisops.gemini.api-key}")
    private String apiKey;

    @Value("${aegisops.gemini.model}")
    private String model;

    @Value("${aegisops.openai.max-tokens:200}")
    private int maxTokens;

    @Value("${aegisops.openai.temperature:0.2}")
    private double temperature;

    public DiagnosisResult analyze(Incident incident) {
        try {
            String prompt = buildPrompt(incident);

            String response = callGemini(prompt).block(); // outer block is ok
            return parseResponse(response);

        } catch (Exception e) {
            log.error("LLM analysis failed", e);
            return DiagnosisResult.builder()
                    .rootCause("Unable to diagnose - LLM analysis failed")
                    .confidence(0.0)
                    .recommendedAction("MANUAL_INTERVENTION")
                    .reasoning("LLM error: " + e.getMessage())
                    .safe(false)
                    .build();
        }
    }

    private String buildPrompt(Incident incident) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an SRE assistant analyzing a Kubernetes incident.\n\n");

        prompt.append("Alert: ").append(incident.getAlertName()).append("\n");
        prompt.append("Namespace: ").append(incident.getNamespace()).append("\n");
        prompt.append("Pod: ").append(incident.getPodName()).append("\n");
        prompt.append("Severity: ").append(incident.getSeverity()).append("\n\n");

        if (incident.getDescription() != null) {
            prompt.append("Description: ").append(incident.getDescription()).append("\n\n");
        }

        if (incident.getPodLogs() != null) {
            String truncatedLogs = truncate(incident.getPodLogs(), 1500);
            prompt.append("Pod Logs (last lines):\n").append(truncatedLogs).append("\n\n");
        }

        if (incident.getPodEvents() != null) {
            prompt.append("Pod Events:\n").append(incident.getPodEvents()).append("\n\n");
        }

        prompt.append("Respond ONLY with valid JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"rootCause\": \"brief explanation\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"recommendedAction\": \"RESTART_POD | SCALE_DEPLOYMENT | SCALE_MEMORY | ROLLOUT_RESTART | MANUAL_INTERVENTION\",\n");
        prompt.append("  \"reasoning\": \"why\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    //Gemini Reactive API Call
    private Mono<String> callGemini(String prompt) {

        WebClient client = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxTokens
                )
        );

        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + model + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        return root.path("candidates")
                                .get(0)
                                .path("content")
                                .path("parts")
                                .get(0)
                                .path("text")
                                .asText();
                    } catch (Exception e) {
                        log.error("Gemini response parsing failed", e);
                        return "{\"rootCause\":\"Parse failed\",\"confidence\":0.0,\"recommendedAction\":\"MANUAL_INTERVENTION\",\"reasoning\":\"Gemini returned unexpected output\"}";
                    }
                })
                .onErrorResume(e -> {
                    log.error("Gemini API failed", e);
                    return Mono.just("{\"rootCause\":\"Gemini unavailable\",\"confidence\":0.0,\"recommendedAction\":\"MANUAL_INTERVENTION\",\"reasoning\":\"LLM quota or network error\"}");
                });
    }

    private DiagnosisResult parseResponse(String response) {
        try {
            JsonNode json = objectMapper.readTree(response.trim());

            return DiagnosisResult.builder()
                    .rootCause(json.path("rootCause").asText("Unknown"))
                    .confidence(json.path("confidence").asDouble(0.5))
                    .recommendedAction(json.path("recommendedAction").asText("MANUAL_INTERVENTION"))
                    .reasoning(json.path("reasoning").asText(""))
                    .safe(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", response, e);
            return DiagnosisResult.builder()
                    .rootCause("Unable to parse diagnosis")
                    .confidence(0.0)
                    .recommendedAction("MANUAL_INTERVENTION")
                    .reasoning("Parse error")
                    .safe(false)
                    .build();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return "..." + text.substring(text.length() - maxLength);
    }
}
