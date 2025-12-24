package io.aegisops.agent.analysis;

import io.aegisops.agent.incident.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiagnosisEngine {
    
    private final RuleBasedAnalyzer ruleBasedAnalyzer;
    private final LlmAnalyzer llmAnalyzer;
    
    @Value("${aegisops.safety.confidence-threshold}")
    private double confidenceThreshold;
    
    @Value("${aegisops.safety.allowed-namespaces}")
    private String allowedNamespaces;
    
    public DiagnosisResult diagnose(Incident incident) {
        log.info("Starting diagnosis for incident: {}", incident.getId());
        
        // Try deterministic rules first
        DiagnosisResult ruleResult = ruleBasedAnalyzer.analyze(incident);
        
        if (ruleResult != null && ruleResult.getConfidence() >= confidenceThreshold) {
            log.info("Rule-based diagnosis succeeded with confidence: {}", ruleResult.getConfidence());
            ruleResult.setSource("RULE_ENGINE");
            return applySafetyChecks(ruleResult, incident);
        }
        
        // Fall back to LLM analysis
        log.info("Rule-based diagnosis inconclusive, using LLM");
        DiagnosisResult llmResult = llmAnalyzer.analyze(incident);
        llmResult.setSource("LLM");
        
        return applySafetyChecks(llmResult, incident);
    }
    
    private DiagnosisResult applySafetyChecks(DiagnosisResult result, Incident incident) {
        boolean safe = true;
        
        // Check confidence threshold
        if (result.getConfidence() < confidenceThreshold) {
            log.warn("Confidence {} below threshold {}", result.getConfidence(), confidenceThreshold);
            safe = false;
        }
        
        // Check namespace whitelist
        if (!isNamespaceAllowed(incident.getNamespace())) {
            log.warn("Namespace {} not in allowed list", incident.getNamespace());
            safe = false;
        }
        
        // Check action is not destructive
        if (isDestructiveAction(result.getRecommendedAction())) {
            log.warn("Destructive action {} rejected", result.getRecommendedAction());
            safe = false;
        }
        
        result.setSafe(safe);
        return result;
    }
    
    private boolean isNamespaceAllowed(String namespace) {
        if (allowedNamespaces == null || allowedNamespaces.isBlank()) {
            return true;
        }
        return allowedNamespaces.contains(namespace);
    }
    
    private boolean isDestructiveAction(String action) {
        if (action == null) return true;
        
        String upper = action.toUpperCase();
        return upper.contains("DELETE") || 
               upper.contains("TERMINATE") || 
               upper.contains("DESTROY");
    }
}