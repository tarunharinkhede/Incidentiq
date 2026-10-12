package com.example.Incidentiq.controller;

import com.example.Incidentiq.dto.DashboardResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.Incidentiq.service.IncidentService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    private final IncidentService incidentService;

    public DashboardController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }
    @GetMapping
    public DashboardResponse dashboard(){
        return incidentService.dashboard();
    }
}
