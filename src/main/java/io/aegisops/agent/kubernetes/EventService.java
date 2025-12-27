package io.aegisops.agent.kubernetes;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final KubernetesClient kubernetesClient;
    
    public String getPodEvents(String namespace, String podName) {
        try {
            List<Event> events = kubernetesClient.v1().events()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .filter(e -> e.getInvolvedObject() != null && 
                            podName.equals(e.getInvolvedObject().getName()))
                .sorted((e1, e2) -> {
                    if (e1.getLastTimestamp() == null) return 1;
                    if (e2.getLastTimestamp() == null) return -1;
                    return e2.getLastTimestamp().compareTo(e1.getLastTimestamp());
                })
                .limit(20)
                .toList();
            
            if (events.isEmpty()) {
                return "No events found";
            }
            
            return events.stream()
                .map(e -> String.format("[%s] %s: %s - %s",
                    e.getType(),
                    e.getReason(),
                    e.getMessage(),
                    e.getLastTimestamp()))
                .collect(Collectors.joining("\n"));
                
        } catch (Exception e) {
            log.warn("Failed to get events for pod {}/{}: {}", namespace, podName, e.getMessage());
            return "Error fetching events: " + e.getMessage();
        }
    }
}
