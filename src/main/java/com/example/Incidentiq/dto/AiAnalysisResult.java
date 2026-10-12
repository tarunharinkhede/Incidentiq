package com.example.Incidentiq.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"summary", "severity", "category", "rootCause", "suggestedFix", "checklist"})
public record AiAnalysisResult (
    String summary,
    String severity,
    String category,
    String rootCause,
    String suggestedFix,
    List<String> checklist
) {}
