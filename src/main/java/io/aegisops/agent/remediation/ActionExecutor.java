package io.aegisops.agent.remediation;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aegisops.agent.incident.Incident;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActionExecutor {
    
    private final KubernetesClient kubernetesClient;
    
    @Value("${aegisops.safety.max-replicas}")
    private int maxReplicas;
    
    @Value("${aegisops.remediation.enabled-actions}")
    private String enabledActions;
    
    public RemediationResult executeAction(Incident incident, String action, boolean dryRun) {
        log.info("Executing action: {} for incident: {} (dryRun: {})", action, incident.getId(), dryRun);
        
        if (!isActionEnabled(action)) {
            return RemediationResult.builder()
                .success(false)
                .action(action)
                .errorMessage("Action " + action + " is not enabled")
                .timestamp(Instant.now())
                .build();
        }
        
        try {
            return switch (action.toUpperCase()) {
                case "RESTART_POD" -> restartPod(incident, dryRun);
                case "SCALE_DEPLOYMENT" -> scaleDeployment(incident, dryRun);
                case "ROLLOUT_RESTART" -> rolloutRestart(incident, dryRun);
                case "SCALE_MEMORY" -> scaleMemory(incident, dryRun);
                default -> RemediationResult.builder()
                    .success(false)
                    .action(action)
                    .errorMessage("Unknown action: " + action)
                    .timestamp(Instant.now())
                    .build();
            };
            
        } catch (Exception e) {
            log.error("Action execution failed", e);
            return RemediationResult.builder()
                .success(false)
                .action(action)
                .errorMessage("Execution error: " + e.getMessage())
                .timestamp(Instant.now())
                .build();
        }
    }
    
    private RemediationResult restartPod(Incident incident, boolean dryRun) {
        if (incident.getPodName() == null) {
            return failedResult("RESTART_POD", "Pod name is null");
        }
        
        if (dryRun) {
            log.info("DRY RUN: Would delete pod {}/{}", incident.getNamespace(), incident.getPodName());
            return successResult("RESTART_POD", "Dry run: pod would be deleted");
        }
        
        kubernetesClient.pods()
            .inNamespace(incident.getNamespace())
            .withName(incident.getPodName())
            .delete();
        
        log.info("Deleted pod {}/{}", incident.getNamespace(), incident.getPodName());
        
        return successResult("RESTART_POD", 
            "Pod deleted successfully. Deployment will recreate it.");
    }
    
    private RemediationResult scaleDeployment(Incident incident, boolean dryRun) {
        if (incident.getDeploymentName() == null) {
            return failedResult("SCALE_DEPLOYMENT", "Deployment name is null");
        }
        
        Deployment deployment = kubernetesClient.apps().deployments()
            .inNamespace(incident.getNamespace())
            .withName(incident.getDeploymentName())
            .get();
        
        if (deployment == null) {
            return failedResult("SCALE_DEPLOYMENT", "Deployment not found");
        }
        
        int currentReplicas = deployment.getSpec().getReplicas();
        int newReplicas = Math.min(currentReplicas + 1, maxReplicas);
        
        if (newReplicas == currentReplicas) {
            return failedResult("SCALE_DEPLOYMENT", 
                "Already at max replicas: " + maxReplicas);
        }
        
        if (dryRun) {
            log.info("DRY RUN: Would scale {}/{} from {} to {} replicas", 
                incident.getNamespace(), incident.getDeploymentName(), 
                currentReplicas, newReplicas);
            return successResult("SCALE_DEPLOYMENT", 
                String.format("Dry run: would scale from %d to %d replicas", 
                    currentReplicas, newReplicas));
        }
        
        kubernetesClient.apps().deployments()
            .inNamespace(incident.getNamespace())
            .withName(incident.getDeploymentName())
            .scale(newReplicas);
        
        log.info("Scaled {}/{} from {} to {} replicas", 
            incident.getNamespace(), incident.getDeploymentName(), 
            currentReplicas, newReplicas);
        
        return successResult("SCALE_DEPLOYMENT", 
            String.format("Scaled from %d to %d replicas", currentReplicas, newReplicas));
    }
    
    private RemediationResult rolloutRestart(Incident incident, boolean dryRun) {
        if (incident.getDeploymentName() == null) {
            return failedResult("ROLLOUT_RESTART", "Deployment name is null");
        }
        
        if (dryRun) {
            log.info("DRY RUN: Would restart deployment {}/{}", 
                incident.getNamespace(), incident.getDeploymentName());
            return successResult("ROLLOUT_RESTART", "Dry run: deployment would be restarted");
        }
        
        kubernetesClient.apps().deployments()
            .inNamespace(incident.getNamespace())
            .withName(incident.getDeploymentName())
            .rolling()
            .restart();
        
        log.info("Rollout restart triggered for {}/{}", 
            incident.getNamespace(), incident.getDeploymentName());
        
        return successResult("ROLLOUT_RESTART", "Rollout restart triggered");
    }
    
    private RemediationResult scaleMemory(Incident incident, boolean dryRun) {
        // This is a placeholder - actual memory scaling requires deployment spec changes
        log.warn("SCALE_MEMORY action requires manual deployment update");
        
        return RemediationResult.builder()
            .success(false)
            .action("SCALE_MEMORY")
            .message("Memory scaling requires manual intervention - update deployment spec")
            .errorMessage("Automated memory scaling not implemented")
            .timestamp(Instant.now())
            .build();
    }
    
    private boolean isActionEnabled(String action) {
        return enabledActions.toUpperCase().contains(action.toUpperCase());
    }
    
    private RemediationResult successResult(String action, String message) {
        return RemediationResult.builder()
            .success(true)
            .action(action)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    private RemediationResult failedResult(String action, String error) {
        return RemediationResult.builder()
            .success(false)
            .action(action)
            .errorMessage(error)
            .timestamp(Instant.now())
            .build();
    }
}
