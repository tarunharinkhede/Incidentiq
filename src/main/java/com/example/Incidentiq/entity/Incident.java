package com.example.Incidentiq.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "incidents")
@Builder
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "service_name", nullable = false)
    private String service;

    @Lob
    @Column(name = "log_text", columnDefinition = "LONGTEXT", nullable = false)
    private String log;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    private String severity;

    private String category;

    @Lob
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Lob
    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;



    @Lob
    @Column(name = "checklist", columnDefinition = "TEXT")
    private String checklist;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "matched_incident_id")
    private Long matchedIncidentId;

    @Column(name ="similarity_score")
    private Double similarityScore;

    @Lob
    @Column(name ="resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if(this.status == null){
            this.status = IncidentStatus.OPEN;
        }
    }

}
