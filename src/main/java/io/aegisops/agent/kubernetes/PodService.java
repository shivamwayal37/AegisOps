package io.aegisops.agent.kubernetes;

import java.util.List;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class PodService {
    
    private final KubernetesClient kubernetesClient;
    
    public Pod getPod(String namespace, String podName) {
        try {
            return kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .get();
        } catch (Exception e) {
            log.error("Failed to get pod {}/{}: {}", namespace, podName, e.getMessage());
            return null;
        }
    }
    
    public List<Pod> listPods(String namespace) {
        try {
            return kubernetesClient.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
        } catch (Exception e) {
            log.error("Failed to list pods in namespace {}: {}", namespace, e.getMessage());
            return List.of();
        }
    }
    
    public List<Pod> listPodsByLabel(String namespace, String labelKey, String labelValue) {
        try {
            return kubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel(labelKey, labelValue)
                .list()
                .getItems();
        } catch (Exception e) {
            log.error("Failed to list pods by label in namespace {}: {}", namespace, e.getMessage());
            return List.of();
        }
    }
    
    public boolean deletePod(String namespace, String podName) {
        try {
            List<StatusDetails> statusDetails = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .delete();
            
            boolean deleted = statusDetails != null && !statusDetails.isEmpty();
            
            if (deleted) {
                log.info("Successfully deleted pod {}/{}", namespace, podName);
            } else {
                log.warn("Failed to delete pod {}/{}", namespace, podName);
            }
            
            return deleted;
        } catch (Exception e) {
            log.error("Error deleting pod {}/{}: {}", namespace, podName, e.getMessage());
            return false;
        }
    }
    
    public String getPodStatus(String namespace, String podName) {
        try {
            Pod pod = getPod(namespace, podName);
            if (pod == null) {
                return "NotFound";
            }
            return pod.getStatus().getPhase();
        } catch (Exception e) {
            log.error("Failed to get pod status for {}/{}: {}", namespace, podName, e.getMessage());
            return "Unknown";
        }
    }
    
    public int getRestartCount(String namespace, String podName) {
        try {
            Pod pod = getPod(namespace, podName);
            if (pod == null || pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
                return 0;
            }
            
            return pod.getStatus().getContainerStatuses()
                .stream()
                .mapToInt(status -> status.getRestartCount())
                .sum();
                
        } catch (Exception e) {
            log.error("Failed to get restart count for {}/{}: {}", namespace, podName, e.getMessage());
            return 0;
        }
    }
}