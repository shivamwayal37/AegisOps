package io.aegisops.agent.kubernetes;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class DeploymentService {
    
    private final KubernetesClient kubernetesClient;
    
    public int getCurrentReplicas(String namespace, String deploymentName) {
        var deployment = kubernetesClient.apps().deployments()
            .inNamespace(namespace)
            .withName(deploymentName)
            .get();
        
        return deployment != null ? deployment.getSpec().getReplicas() : 0;
    }
}
