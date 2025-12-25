package io.aegisops.agent.approval;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approvals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ApprovalRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String incidentId;
    
    @Column(nullable = false)
    private String recommendedAction;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    
    private Double confidence;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;
    
    private String approvedBy;
    
    @Column(nullable = false)
    private Instant requestedAt;
    
    private Instant respondedAt;
    
    @PrePersist
    protected void onCreate() {
        requestedAt = Instant.now();
        if (status == null) {
            status = ApprovalStatus.PENDING;
        }
    }
    
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }
}
