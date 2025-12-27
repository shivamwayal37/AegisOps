package io.aegisops.agent.kubernetes;

import java.util.List;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class DeploymentService {
    
    private final KubernetesClient kubernetesClient;
    
    public Deployment getDeployment(String namespace, String deploymentName) {
        try {
            return kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .get();
        } catch (Exception e) {
            log.error("Failed to get deployment {}/{}: {}", namespace, deploymentName, e.getMessage());
            return null;
        }
    }
    
    public int getCurrentReplicas(String namespace, String deploymentName) {
        try {
            Deployment deployment = getDeployment(namespace, deploymentName);
            if (deployment == null || deployment.getSpec() == null) {
                return 0;
            }
            return deployment.getSpec().getReplicas();
        } catch (Exception e) {
            log.error("Failed to get replica count for {}/{}: {}", namespace, deploymentName, e.getMessage());
            return 0;
        }
    }
    
    public int getReadyReplicas(String namespace, String deploymentName) {
        try {
            Deployment deployment = getDeployment(namespace, deploymentName);
            if (deployment == null || deployment.getStatus() == null) {
                return 0;
            }
            Integer ready = deployment.getStatus().getReadyReplicas();
            return ready != null ? ready : 0;
        } catch (Exception e) {
            log.error("Failed to get ready replicas for {}/{}: {}", namespace, deploymentName, e.getMessage());
            return 0;
        }
    }
    
    public boolean scaleDeployment(String namespace, String deploymentName, int replicas) {
        try {
            kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .scale(replicas);
            
            log.info("Successfully scaled deployment {}/{} to {} replicas", 
                namespace, deploymentName, replicas);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to scale deployment {}/{}: {}", namespace, deploymentName, e.getMessage());
            return false;
        }
    }
    
    public boolean rolloutRestart(String namespace, String deploymentName) {
        try {
            kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .rolling()
                .restart();
            
            log.info("Successfully triggered rollout restart for deployment {}/{}", 
                namespace, deploymentName);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to restart deployment {}/{}: {}", namespace, deploymentName, e.getMessage());
            return false;
        }
    }
    
    public List<Deployment> listDeployments(String namespace) {
        try {
            return kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .list()
                .getItems();
        } catch (Exception e) {
            log.error("Failed to list deployments in namespace {}: {}", namespace, e.getMessage());
            return List.of();
        }
    }
    
    public boolean updateDeploymentImage(String namespace, String deploymentName, 
                                         String containerName, String newImage) {
        try {
            kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .edit(deployment -> {
                    deployment.getSpec().getTemplate().getSpec().getContainers()
                        .stream()
                        .filter(c -> c.getName().equals(containerName))
                        .findFirst()
                        .ifPresent(c -> c.setImage(newImage));
                    return deployment;
                });
            
            log.info("Successfully updated image for deployment {}/{}", namespace, deploymentName);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to update deployment image {}/{}: {}", 
                namespace, deploymentName, e.getMessage());
            return false;
        }
    }
}