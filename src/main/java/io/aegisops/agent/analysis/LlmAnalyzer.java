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

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmAnalyzer {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${aegisops.openai.api-key}")
    private String apiKey;
    
    @Value("${aegisops.openai.model}")
    private String model;
    
    @Value("${aegisops.openai.max-tokens}")
    private int maxTokens;
    
    @Value("${aegisops.openai.temperature}")
    private double temperature;
    
    public DiagnosisResult analyze(Incident incident) {
        try {
            String prompt = buildPrompt(incident);
            String response = callOpenAI(prompt);
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
        
        prompt.append("Analyze this incident and respond ONLY with valid JSON (no markdown, no backticks):\n");
        prompt.append("{\n");
        prompt.append("  \"rootCause\": \"brief root cause explanation\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"recommendedAction\": \"RESTART_POD | SCALE_DEPLOYMENT | SCALE_MEMORY | ROLLOUT_RESTART | MANUAL_INTERVENTION\",\n");
        prompt.append("  \"reasoning\": \"why this action is recommended\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private String callOpenAI(String prompt) {
        WebClient client = webClientBuilder
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("Content-Type", "application/json")
            .build();
        
        Map<String, Object> request = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "temperature", temperature,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );
        
        try {
            String response = client.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            
            if (content.isArray() && content.size() > 0) {
                return content.get(0).path("text").asText();
            }
            
            throw new RuntimeException("Unexpected API response format");
            
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("LLM API error: " + e.getMessage());
        }
    }
    
    private DiagnosisResult parseResponse(String response) {
        try {
            // Remove markdown code blocks if present
            String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            
            JsonNode json = objectMapper.readTree(cleaned);
            
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
