package com.example.Incidentiq.dto;

import com.example.Incidentiq.entity.IncidentStatus;
import com.example.Incidentiq.entity.Incident;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record IncidentResponse (

    Long id,
    String title,
    String service,
    String log,
    String summary,
    String severity,
    String category,
    String rootCause,
    String suggestion,
    List<String> checklist,
    IncidentStatus status,
    Long matchedIncidentId,
    Double similarityScore,
    String resolutionNotes,
    String resolvedBy,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {
        public static IncidentResponse from(Incident i) {
            List<String> checklist = (i.getChecklist() == null || i.getChecklist().isBlank())
                    ? List.of()
                    : Arrays.asList(i.getChecklist().split("\n"));

            return new IncidentResponse(
                    i.getId(), i.getTitle(), i.getService(), i.getLog(),
                    i.getSummary(), i.getSeverity(), i.getCategory(), i.getRootCause(),
                    i.getSuggestion(), checklist, i.getStatus(),
                    i.getMatchedIncidentId(), i.getSimilarityScore(),
                    i.getResolutionNotes(), i.getResolvedBy(),
                    i.getCreatedAt(), i.getResolvedAt()
            );
        }
}
