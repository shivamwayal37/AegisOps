package io.aegisops.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KubernetesClientConfig {
    
    @Value("${aegisops.kubernetes.in-cluster}")
    private boolean inCluster;
    
    @Bean
    public KubernetesClient kubernetesClient() {
        Config config;
        
        if (inCluster) {
            log.info("Initializing in-cluster Kubernetes client");
            config = new ConfigBuilder()
                .build();
        } else {
            log.info("Initializing local Kubernetes client (using kubeconfig)");
            config = Config.autoConfigure(null);
        }
        
        KubernetesClient client = new KubernetesClientBuilder()
            .withConfig(config)
            .build();
        
        log.info("Kubernetes client initialized - namespace: {}", 
            client.getNamespace());
        
        return client;
    }
}
