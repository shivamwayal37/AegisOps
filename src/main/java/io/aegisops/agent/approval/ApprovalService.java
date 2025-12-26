package io.aegisops.agent.approval;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.aegisops.agent.analysis.DiagnosisResult;
import io.aegisops.agent.incident.Incident;
import io.aegisops.agent.metrics.MetricsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {
    
    private final ApprovalRepository approvalRepository;
    private final MetricsService metricsService;
    
    @Transactional
    public ApprovalRequest requestApproval(Incident incident, DiagnosisResult diagnosis) {
        ApprovalRequest request = ApprovalRequest.builder()
            .incidentId(incident.getId())
            .recommendedAction(diagnosis.getRecommendedAction())
            .reasoning(diagnosis.getReasoning())
            .confidence(diagnosis.getConfidence())
            .status(ApprovalRequest.ApprovalStatus.PENDING)
            .build();
        
        request = approvalRepository.save(request);
        metricsService.incrementApprovalsPending();
        
        log.info("Approval requested for incident: {}, action: {}", 
            incident.getId(), diagnosis.getRecommendedAction());
        
        return request;
    }
    
    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRepository.findByStatus(ApprovalRequest.ApprovalStatus.PENDING);
    }
    
    public Optional<ApprovalRequest> findById(String id) {
        return approvalRepository.findById(id);
    }
}