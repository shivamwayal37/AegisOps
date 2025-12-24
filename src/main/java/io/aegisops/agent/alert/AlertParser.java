package io.aegisops.agent.alert;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.aegisops.agent.incident.Incident;

@Component
public class AlertParser {

    public Incident parse(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> alert = ((List<Map<String, Object>>) payload.get("alerts")).get(0);
        @SuppressWarnings("unchecked")
        Map<String, String> labels = (Map<String, String>) alert.get("labels");

        return Incident.builder()
                .alertName(labels.get("alertname"))
                .namespace(labels.get("namespace"))
                .podName(labels.get("pod"))
                .severity(labels.get("severity"))
                .build();
    }
}

