package io.aegisops.agent.alert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.aegisops.agent.incident.Incident;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AlertParser {
    
    @SuppressWarnings("unchecked")
    public List<Incident> parseAlertPayload(Map<String, Object> payload) {
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.getOrDefault("alerts", List.of());
        
        return alerts.stream()
            .filter(this::isValidAlert)
            .map(this::parseAlert)
            .collect(Collectors.toList());
    }
    
    private boolean isValidAlert(Map<String, Object> alert) {
        String status = (String) alert.get("status");
        return "firing".equalsIgnoreCase(status);
    }
    
    @SuppressWarnings("unchecked")
    private Incident parseAlert(Map<String, Object> alert) {
        Map<String, String> labels = (Map<String, String>) alert.getOrDefault("labels", Map.of());
        Map<String, String> annotations = (Map<String, String>) alert.getOrDefault("annotations", Map.of());
        
        String alertName = labels.getOrDefault("alertname", "UnknownAlert");
        String namespace = labels.getOrDefault("namespace", "default");
        String podName = labels.getOrDefault("pod", labels.getOrDefault("pod_name", null));
        String deploymentName = extractDeploymentName(podName);
        String severity = labels.getOrDefault("severity", "warning");
        String description = annotations.getOrDefault("description", annotations.getOrDefault("summary", ""));
        
        Map<String, String> metrics = new HashMap<>();
        labels.forEach((k, v) -> {
            if (k.startsWith("metric_") || k.contains("value") || k.contains("threshold")) {
                metrics.put(k, v);
            }
        });
        
        return Incident.builder()
            .alertName(alertName)
            .namespace(namespace)
            .podName(podName)
            .deploymentName(deploymentName)
            .severity(severity)
            .description(description)
            .metrics(metrics)
            .status(Incident.IncidentStatus.NEW)
            .build();
    }
    
    private String extractDeploymentName(String podName) {
        if (podName == null) return null;
        
        // Remove replica set hash and pod hash
        // Example: myapp-deployment-7d8f9c5b4-xyz123 -> myapp-deployment
        int lastDash = podName.lastIndexOf('-');
        if (lastDash > 0) {
            String withoutPodHash = podName.substring(0, lastDash);
            lastDash = withoutPodHash.lastIndexOf('-');
            if (lastDash > 0) {
                return withoutPodHash.substring(0, lastDash);
            }
        }
        
        return podName;
    }
}

