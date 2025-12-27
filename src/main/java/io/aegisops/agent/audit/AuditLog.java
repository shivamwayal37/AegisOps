package io.aegisops.agent.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String incidentId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    
    private String approvedBy;
    
    @Column(nullable = false)
    private boolean success;
    
    @Column(columnDefinition = "TEXT")
    private String result;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}