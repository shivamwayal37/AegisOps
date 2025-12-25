package io.aegisops.agent.incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    List<Incident> findByNamespace(String namespace);
    List<Incident> findByStatus(Incident.IncidentStatus status);
    List<Incident> findByNamespaceAndStatus(String namespace, Incident.IncidentStatus status);
}
