package io.aegisops.agent.approval;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, String> {
    List<ApprovalRequest> findByStatus(ApprovalRequest.ApprovalStatus status);
    Optional<ApprovalRequest> findByIncidentId(String incidentId);
}
