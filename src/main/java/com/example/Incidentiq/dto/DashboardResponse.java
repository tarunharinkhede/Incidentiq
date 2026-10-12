package com.example.Incidentiq.dto;

import java.util.Map;

public record DashboardResponse(
        long total,
        long open,
        long resolved,
        long critical,
        Map<String, Long> byCategory,
        Map<String, Long> bySeverity
) {}
