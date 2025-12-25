package io.aegisops.agent.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsService {
    
    private final Counter alertsReceived;
    private final Counter alertsFailed;
    private final Counter actionsSuccess;
    private final Counter actionsFailed;
    private final AtomicInteger approvalsPending;
    private final Timer mttrTimer;
    
    public MetricsService(MeterRegistry registry) {
        this.alertsReceived = Counter.builder("aegisops.alerts.received")
            .description("Total alerts received")
            .register(registry);
        
        this.alertsFailed = Counter.builder("aegisops.alerts.failed")
            .description("Alerts that failed processing")
            .register(registry);
        
        this.actionsSuccess = Counter.builder("aegisops.actions.success")
            .description("Successful remediation actions")
            .register(registry);
        
        this.actionsFailed = Counter.builder("aegisops.actions.failed")
            .description("Failed remediation actions")
            .register(registry);
        
        this.approvalsPending = new AtomicInteger(0);
        Gauge.builder("aegisops.approvals.pending", approvalsPending, AtomicInteger::get)
            .description("Pending approval requests")
            .register(registry);
        
        this.mttrTimer = Timer.builder("aegisops.mttr")
            .description("Mean Time To Recovery")
            .register(registry);
    }
    
    public void incrementAlertsReceived() {
        alertsReceived.increment();
    }
    
    public void incrementAlertsFailed() {
        alertsFailed.increment();
    }
    
    public void incrementActionsSuccess() {
        actionsSuccess.increment();
    }
    
    public void incrementActionsFailed() {
        actionsFailed.increment();
    }
    
    public void incrementApprovalsPending() {
        approvalsPending.incrementAndGet();
    }
    
    public void decrementApprovalsPending() {
        approvalsPending.decrementAndGet();
    }
    
    public void recordMTTR(long milliseconds) {
        mttrTimer.record(milliseconds, TimeUnit.MILLISECONDS);
        log.info("MTTR recorded: {}ms", milliseconds);
    }
}
