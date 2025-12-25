package io.aegisops.agent.incident;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.*;


@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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