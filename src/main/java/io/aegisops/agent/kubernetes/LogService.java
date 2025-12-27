package io.aegisops.agent.kubernetes;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {
    
    private final KubernetesClient kubernetesClient;
    
    public String getPodLogs(String namespace, String podName, int lines) {
        try {
            String logs = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .tailingLines(lines)
                .getLog();
            
            return logs != null ? logs : "No logs available";
            
        } catch (Exception e) {
            log.warn("Failed to get logs for pod {}/{}: {}", namespace, podName, e.getMessage());
            return "Error fetching logs: " + e.getMessage();
        }
    }
    
    public String getContainerLogs(String namespace, String podName, String containerName, int lines) {
        try {
            String logs = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .tailingLines(lines)
                .getLog();
            
            return logs != null ? logs : "No logs available";
            
        } catch (Exception e) {
            log.warn("Failed to get logs for container {}/{}/{}: {}", 
                namespace, podName, containerName, e.getMessage());
            return "Error fetching container logs: " + e.getMessage();
        }
    }
}
