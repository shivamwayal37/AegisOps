package io.aegisops.agent.kubernetes;

import java.util.List;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class PodService {
    
    private final KubernetesClient kubernetesClient;
    
    public Pod getPod(String namespace, String podName) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .get();
    }
    
    public List<Pod> listPods(String namespace) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .list()
            .getItems();
    }
}