package com.example.Incidentiq.dto;

import com.example.Incidentiq.entity.Incident;

public record SimilarIncidentMatch(
    Incident incident,
    double score
){}
