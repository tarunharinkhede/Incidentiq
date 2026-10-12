package com.example.Incidentiq.dto;

import jakarta.validation.constraints.NotBlank;
public record IncidentCreateRequest (
    @NotBlank(message = "title is required") String title,
    @NotBlank(message = "service is required") String service,
    @NotBlank(message = "log is required") String log
) {}
