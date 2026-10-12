package com.example.Incidentiq.dto;
import jakarta.validation.constraints.NotBlank;
public record ResolveRequest (
    @NotBlank(message = "resolutionNotes is required") String resolutionNotes,
    String resolvedBy
){}
