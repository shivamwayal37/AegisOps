package io.aegisops.agent.audit;

import java.util.List;

import org.springframework.stereotype.Service;

import io.aegisops.agent.analysis.DiagnosisResult;
import io.aegisops.agent.incident.Incident;
import io.aegisops.agent.remediation.RemediationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditRepository auditRepository;
    
    public void logAction(Incident incident, DiagnosisResult diagnosis, RemediationResult result) {
        AuditLog auditLog = AuditLog.builder()
            .incidentId(incident.getId())
            .action(result.getAction())
            .reasoning(diagnosis.getReasoning())
            .approvedBy("system") // Can be enhanced to track actual approver
            .success(result.isSuccess())
            .result(result.getMessage() != null ? result.getMessage() : result.getErrorMessage())
            .build();
        
        auditRepository.save(auditLog);
        
        log.info("Audit log created for incident: {}, action: {}, success: {}", 
            incident.getId(), result.getAction(), result.isSuccess());
    }
    
    public List<AuditLog> getAuditLogs(String incidentId) {
        return auditRepository.findByIncidentId(incidentId);
    }
}
