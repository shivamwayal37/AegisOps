package io.aegisops.agent.incident;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    
    @Id
    @UuidGenerator
    private String id;
    
    @Column(nullable = false)
    private String alertName;
    
    @Column(nullable = false)
    private String namespace;
    
    private String podName;
    
    private String deploymentName;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String podLogs;
    
    @Column(columnDefinition = "TEXT")
    private String podEvents;
    
    @ElementCollection
    @CollectionTable(name = "incident_metrics", joinColumns = @JoinColumn(name = "incident_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_value")
    private Map<String, String> metrics;
    
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant resolvedAt;
    
    @Column(columnDefinition = "TEXT")
    private String rootCause;
    
    private Double diagnosisConfidence;
    
    private String recommendedAction;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = IncidentStatus.NEW;
        }
    }
    
    public enum IncidentStatus {
        NEW,
        ANALYZING,
        PENDING_APPROVAL,
        APPROVED,
        REMEDIATING,
        RESOLVED,
        FAILED
    }
}