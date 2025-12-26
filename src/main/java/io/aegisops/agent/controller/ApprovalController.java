package io.aegisops.agent.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.aegisops.agent.approval.ApprovalRepository;
import io.aegisops.agent.approval.ApprovalRequest;
import io.aegisops.agent.approval.ApprovalService;
import io.aegisops.agent.incident.Incident;
import io.aegisops.agent.incident.IncidentRepository;
import io.aegisops.agent.incident.IncidentService;
import io.aegisops.agent.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
class ApprovalController {
    
    private final ApprovalService approvalService;
    private final ApprovalRepository approvalRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final MetricsService metricsService;
    
    @GetMapping
    public ResponseEntity<List<ApprovalRequest>> listPendingApprovals() {
        List<ApprovalRequest> pending = approvalService.getPendingApprovals();
        return ResponseEntity.ok(pending);
    }
    
    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "admin") String approver) {
        
        Optional<ApprovalRequest> requestOpt = approvalService.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApprovalRequest request = requestOpt.get();
        request.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        request.setApprovedBy(approver);
        request.setRespondedAt(Instant.now());
        approvalRepository.save(request);
        
        metricsService.decrementApprovalsPending();
        
        // Trigger remediation
        Optional<Incident> incidentOpt = incidentRepository.findById(request.getIncidentId());
        if (incidentOpt.isPresent()) {
            Incident incident = incidentOpt.get();
            incident.setStatus(Incident.IncidentStatus.APPROVED);
            incidentRepository.save(incident);
            
            // Execute remediation
            var diagnosis = new io.aegisops.agent.analysis.DiagnosisResult();
            diagnosis.setRecommendedAction(request.getRecommendedAction());
            diagnosis.setConfidence(request.getConfidence());
            diagnosis.setReasoning(request.getReasoning());
            diagnosis.setSafe(true);
            
            incidentService.executeRemediation(incident, diagnosis);
            
            log.info("Approval {} approved by {}, executing remediation", id, approver);
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "approved",
            "approver", approver
        ));
    }
    
    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "admin") String approver) {
        
        Optional<ApprovalRequest> requestOpt = approvalService.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApprovalRequest request = requestOpt.get();
        request.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        request.setApprovedBy(approver);
        request.setRespondedAt(Instant.now());
        approvalRepository.save(request);
        
        metricsService.decrementApprovalsPending();
        
        // Update incident status
        incidentRepository.findById(request.getIncidentId()).ifPresent(incident -> {
            incident.setStatus(Incident.IncidentStatus.FAILED);
            incidentRepository.save(incident);
        });
        
        log.info("Approval {} rejected by {}", id, approver);
        
        return ResponseEntity.ok(Map.of(
            "status", "rejected",
            "approver", approver
        ));
    }
}