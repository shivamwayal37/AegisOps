package io.aegisops.agent.incident;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.aegisops.agent.analysis.DiagnosisEngine;
import io.aegisops.agent.analysis.DiagnosisResult;
import io.aegisops.agent.approval.ApprovalService;
import io.aegisops.agent.audit.AuditService;
import io.aegisops.agent.kubernetes.EventService;
import io.aegisops.agent.kubernetes.LogService;
import io.aegisops.agent.metrics.MetricsService;
import io.aegisops.agent.remediation.ActionExecutor;
import io.aegisops.agent.remediation.RemediationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {
    
    private final IncidentRepository incidentRepository;
    private final LogService logService;
    private final EventService eventService;
    private final DiagnosisEngine diagnosisEngine;
    private final ApprovalService approvalService;
    private final ActionExecutor actionExecutor;
    private final AuditService auditService;
    private final MetricsService metricsService;
    
    @Value("${aegisops.safety.require-approval}")
    private boolean requireApproval;
    
    @Value("${aegisops.safety.dry-run}")
    private boolean dryRun;
    
    @Async
    @Transactional
    public void processIncident(Incident incident) {
        try {
            // Save initial incident
            incident = incidentRepository.save(incident);
            log.info("Created incident: {}", incident.getId());
            
            // Enrich with K8s data
            enrichIncidentData(incident);
            incident.setStatus(Incident.IncidentStatus.ANALYZING);
            incident = incidentRepository.save(incident);
            
            // Diagnose
            DiagnosisResult diagnosis = diagnosisEngine.diagnose(incident);
            incident.setRootCause(diagnosis.getRootCause());
            incident.setDiagnosisConfidence(diagnosis.getConfidence());
            incident.setRecommendedAction(diagnosis.getRecommendedAction());
            incident = incidentRepository.save(incident);
            
            log.info("Diagnosis complete - Confidence: {}, Action: {}", 
                diagnosis.getConfidence(), diagnosis.getRecommendedAction());
            
            // Safety check
            if (!diagnosis.isSafe()) {
                log.warn("Unsafe diagnosis, manual intervention required");
                incident.setStatus(Incident.IncidentStatus.FAILED);
                incidentRepository.save(incident);
                return;
            }
            
            // Handle approval workflow
            if (requireApproval && !dryRun) {
                incident.setStatus(Incident.IncidentStatus.PENDING_APPROVAL);
                incident = incidentRepository.save(incident);
                
                approvalService.requestApproval(incident, diagnosis);
                log.info("Approval requested for incident: {}", incident.getId());
                return;
            }
            
            // Execute remediation
            executeRemediation(incident, diagnosis);
            
        } catch (Exception e) {
            log.error("Error processing incident: {}", incident.getId(), e);
            incident.setStatus(Incident.IncidentStatus.FAILED);
            incidentRepository.save(incident);
            metricsService.incrementActionsFailed();
        }
    }
    
    private void enrichIncidentData(Incident incident) {
        if (incident.getPodName() != null) {
            try {
                String logs = logService.getPodLogs(incident.getNamespace(), incident.getPodName(), 100);
                incident.setPodLogs(logs);
                
                String events = eventService.getPodEvents(incident.getNamespace(), incident.getPodName());
                incident.setPodEvents(events);
                
                log.debug("Enriched incident {} with {} log lines", incident.getId(), 
                    logs != null ? logs.split("\n").length : 0);
            } catch (Exception e) {
                log.warn("Failed to enrich incident data: {}", e.getMessage());
            }
        }
    }
    
    @Transactional
    public void executeRemediation(Incident incident, DiagnosisResult diagnosis) {
        incident.setStatus(Incident.IncidentStatus.REMEDIATING);
        incident = incidentRepository.save(incident);
        
        long startTime = System.currentTimeMillis();
        
        RemediationResult result = actionExecutor.executeAction(
            incident, 
            diagnosis.getRecommendedAction(),
            dryRun
        );
        
        long mttr = System.currentTimeMillis() - startTime;
        
        if (result.isSuccess()) {
            incident.setStatus(Incident.IncidentStatus.RESOLVED);
            incident.setResolvedAt(Instant.now());
            metricsService.recordMTTR(mttr);
            metricsService.incrementActionsSuccess();
            log.info("Incident {} resolved in {}ms", incident.getId(), mttr);
        } else {
            incident.setStatus(Incident.IncidentStatus.FAILED);
            metricsService.incrementActionsFailed();
            log.error("Failed to remediate incident {}: {}", incident.getId(), result.getErrorMessage());
        }
        
        incidentRepository.save(incident);
        
        auditService.logAction(incident, diagnosis, result);
    }
    
    public List<Incident> findIncidents(String namespace, String status) {
        if (namespace != null && status != null) {
            return incidentRepository.findByNamespaceAndStatus(
                namespace, 
                Incident.IncidentStatus.valueOf(status.toUpperCase())
            );
        } else if (namespace != null) {
            return incidentRepository.findByNamespace(namespace);
        } else if (status != null) {
            return incidentRepository.findByStatus(
                Incident.IncidentStatus.valueOf(status.toUpperCase())
            );
        }
        return incidentRepository.findAll();
    }
    
    public Optional<Incident> findById(String id) {
        return incidentRepository.findById(id);
    }
}
