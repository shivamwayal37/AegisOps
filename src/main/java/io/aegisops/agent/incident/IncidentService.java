package io.aegisops.agent.incident;

import java.util.Map;

import org.springframework.stereotype.Service;

import io.aegisops.agent.alert.AlertParser;
import io.aegisops.agent.analysis.DiagnosisEngine;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final AlertParser alertParser;
    private final DiagnosisEngine diagnosisEngine;

    public void handleIncomingAlert(Map<String, Object> payload) {
        Incident incident = alertParser.parse(payload);
        diagnosisEngine.diagnose(incident);
    }
}

