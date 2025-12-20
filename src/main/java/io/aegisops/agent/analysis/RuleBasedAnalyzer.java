package io.aegisops.agent.analysis;

import org.springframework.stereotype.Component;

import io.aegisops.agent.incident.Incident;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RuleBasedAnalyzer {
    
    public DiagnosisResult analyze(Incident incident) {
        log.debug("Applying rule-based analysis for: {}", incident.getAlertName());
        
        String alertName = incident.getAlertName().toLowerCase();
        String events = incident.getPodEvents() != null ? incident.getPodEvents().toLowerCase() : "";
        String logs = incident.getPodLogs() != null ? incident.getPodLogs().toLowerCase() : "";
        
        // Rule 1: OOMKilled
        if (events.contains("oomkilled") || alertName.contains("oom")) {
            return DiagnosisResult.builder()
                .rootCause("Pod was killed due to Out Of Memory. Container memory limit exceeded.")
                .confidence(0.95)
                .recommendedAction("SCALE_MEMORY")
                .reasoning("OOMKilled event detected in pod events")
                .safe(true)
                .build();
        }
        
        // Rule 2: CrashLoopBackOff
        if (events.contains("crashloopbackoff") || events.contains("backoff")) {
            if (logs.contains("error") || logs.contains("exception") || logs.contains("panic")) {
                return DiagnosisResult.builder()
                    .rootCause("Application crashing on startup due to error in logs")
                    .confidence(0.85)
                    .recommendedAction("RESTART_POD")
                    .reasoning("CrashLoopBackOff with error patterns in logs")
                    .safe(true)
                    .build();
            }
            
            return DiagnosisResult.builder()
                .rootCause("Pod in CrashLoopBackOff state")
                .confidence(0.75)
                .recommendedAction("RESTART_POD")
                .reasoning("CrashLoopBackOff detected")
                .safe(true)
                .build();
        }
        
        // Rule 3: High CPU
        if (alertName.contains("cpu") && (alertName.contains("high") || alertName.contains("throttl"))) {
            return DiagnosisResult.builder()
                .rootCause("CPU usage exceeding limits, causing throttling")
                .confidence(0.90)
                .recommendedAction("SCALE_DEPLOYMENT")
                .reasoning("High CPU alert triggered")
                .safe(true)
                .build();
        }
        
        // Rule 4: Pod Pending
        if (events.contains("failedscheduling") || alertName.contains("pending")) {
            return DiagnosisResult.builder()
                .rootCause("Pod cannot be scheduled - insufficient resources")
                .confidence(0.80)
                .recommendedAction("MANUAL_INTERVENTION")
                .reasoning("FailedScheduling event indicates cluster capacity issue")
                .safe(false)
                .build();
        }
        
        // Rule 5: ImagePullBackOff
        if (events.contains("imagepullbackoff") || events.contains("errimagepull")) {
            return DiagnosisResult.builder()
                .rootCause("Cannot pull container image - invalid image or auth issue")
                .confidence(0.95)
                .recommendedAction("MANUAL_INTERVENTION")
                .reasoning("Image pull failure detected")
                .safe(false)
                .build();
        }
        
        // Rule 6: High Memory (not OOM yet)
        if (alertName.contains("memory") && alertName.contains("high")) {
            return DiagnosisResult.builder()
                .rootCause("Memory usage approaching limits")
                .confidence(0.85)
                .recommendedAction("SCALE_MEMORY")
                .reasoning("High memory alert before OOM")
                .safe(true)
                .build();
        }
        
        // Rule 7: Liveness probe failed
        if (events.contains("liveness") && events.contains("fail")) {
            return DiagnosisResult.builder()
                .rootCause("Liveness probe failing - application not responding to health checks")
                .confidence(0.90)
                .recommendedAction("RESTART_POD")
                .reasoning("Liveness probe failures indicate unhealthy container")
                .safe(true)
                .build();
        }
        
        // Rule 8: Readiness probe failed
        if (events.contains("readiness") && events.contains("fail")) {
            return DiagnosisResult.builder()
                .rootCause("Readiness probe failing - application not ready to serve traffic")
                .confidence(0.85)
                .recommendedAction("RESTART_POD")
                .reasoning("Readiness probe failures")
                .safe(true)
                .build();
        }
        
        log.debug("No deterministic rule matched for: {}", incident.getAlertName());
        return null; // No rule matched
    }
}