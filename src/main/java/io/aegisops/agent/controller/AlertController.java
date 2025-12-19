package io.aegisops.agent.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.aegisops.agent.incident.IncidentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<Void> receiveAlert(@RequestBody Map<String, Object> payload) {
        incidentService.handleIncomingAlert(payload);
        return ResponseEntity.ok().build();
    }
}

