package io.aegisops.agent.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.aegisops.agent.alert.AlertParser;
import io.aegisops.agent.incident.Incident;
import io.aegisops.agent.incident.IncidentService;
import io.aegisops.agent.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AlertController {
    
    private final AlertParser alertParser;
    private final IncidentService incidentService;
    private final MetricsService metricsService;
    
    @PostMapping("/alerts")
    public ResponseEntity<Map<String, String>> receiveAlert(@RequestBody Map<String, Object> alertPayload) {
        try {
            log.info("Received alert webhook: {}", alertPayload);
            metricsService.incrementAlertsReceived();
            
            List<Incident> incidents = alertParser.parseAlertPayload(alertPayload);
            
            for (Incident incident : incidents) {
                log.info("Processing incident: {} for pod: {}", incident.getAlertName(), incident.getPodName());
                incidentService.processIncident(incident);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "incidents_created", String.valueOf(incidents.size())
            ));
            
        } catch (Exception e) {
            log.error("Error processing alert", e);
            metricsService.incrementAlertsFailed();
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/incidents")
    public ResponseEntity<List<Incident>> listIncidents(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String status) {
        
        List<Incident> incidents = incidentService.findIncidents(namespace, status);
        return ResponseEntity.ok(incidents);
    }
    
    @GetMapping("/incidents/{id}")
    public ResponseEntity<Incident> getIncident(@PathVariable String id) {
        return incidentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}