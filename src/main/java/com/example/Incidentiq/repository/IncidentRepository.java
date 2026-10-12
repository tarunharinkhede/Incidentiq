package com.example.Incidentiq.repository;

import com.example.Incidentiq.entity.Incident;
import com.example.Incidentiq.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusIn(List<IncidentStatus> statuses);
    long countByStatus(IncidentStatus status);

    List<Incident> findTop5ByCategoryIsNotNullOrderByCreatedAtDesc();
}
