package com.example.Incidentiq.controller;

import com.example.Incidentiq.dto.IncidentResponse;
import com.example.Incidentiq.dto.ResolveRequest;
import com.example.Incidentiq.dto.IncidentCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.Incidentiq.service.IncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService){
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentCreateRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<IncidentResponse> analyze(@PathVariable Long id){
        return ResponseEntity.ok(incidentService.analyze(id));
    }
    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAll(){
        return ResponseEntity.ok(incidentService.getAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getOne(@PathVariable Long id){
        return ResponseEntity.ok(incidentService.getById(id));
    }
    @PutMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request){
        return ResponseEntity.ok(incidentService.resolve(id, request));
    }

}
