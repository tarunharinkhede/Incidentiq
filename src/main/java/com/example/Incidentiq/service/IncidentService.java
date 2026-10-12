package com.example.Incidentiq.service;

import com.example.Incidentiq.dto.AiAnalysisResult;
import com.example.Incidentiq.dto.IncidentCreateRequest;
import com.example.Incidentiq.dto.SimilarIncidentMatch;
import com.example.Incidentiq.dto.DashboardResponse;
import com.example.Incidentiq.dto.ResolveRequest;
import com.example.Incidentiq.exception.ResourceNotFoundException;
import com.example.Incidentiq.entity.Incident;
import com.example.Incidentiq.entity.IncidentStatus;
import com.example.Incidentiq.dto.IncidentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.Incidentiq.repository.IncidentRepository;
import com.example.Incidentiq.service.SimilarityService;
import com.example.Incidentiq.service.AiAnalysisService;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final SimilarityService similarityService;
    private final AiAnalysisService aiAnalysisService;

    public IncidentService(IncidentRepository incidentRepository, SimilarityService similarityService, AiAnalysisService aiAnalysisService){
        this.incidentRepository = incidentRepository;
        this.similarityService = similarityService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Transactional
    public IncidentResponse create(IncidentCreateRequest request){
        Incident incident = Incident.builder()
                .title(request.title())
                .service(request.service())
                .log(request.log())
                .status(IncidentStatus.OPEN)
                .build();

        incident = incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
        public IncidentResponse analyze(Long id) {
        Incident incident = getEntity(id);

        Optional<SimilarIncidentMatch> match = similarityService.findBestmatch(incident.getLog(), incident.getId());

        if (match.isPresent()) {
            Incident source = match.get().incident();
            incident.setSummary(source.getSummary());
            incident.setSeverity(source.getSeverity());
            incident.setCategory(source.getCategory());
            incident.setRootCause(source.getRootCause());
            incident.setSuggestion(source.getSuggestion());
            incident.setChecklist(source.getChecklist());
            incident.setMatchedIncidentId(source.getId());
            incident.setSimilarityScore(match.get().score());
            incident.setStatus(IncidentStatus.ANALYZED);
        } else {
            AiAnalysisResult result = aiAnalysisService.analyze(incident.getService(), incident.getLog());
            incident.setSummary(result.summary());
            incident.setSeverity(result.severity());
            incident.setCategory(result.category());
            incident.setRootCause(result.rootCause());
            incident.setSuggestion(result.suggestedFix());
            incident.setChecklist(result.checklist() == null ? "" : String.join("\n", result.checklist()));
            incident.setMatchedIncidentId(null);
            incident.setSimilarityScore(null);
            incident.setStatus(IncidentStatus.ANALYZED);
        }
    incident = incidentRepository.save(incident);
        return IncidentResponse.from(incident);

    }
    @Transactional(readOnly = true)
    public List<IncidentResponse> getAll(){
        return incidentRepository.findAll().stream()
                .map(IncidentResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id){
        return IncidentResponse.from(getEntity(id));
    }

//    @Transactional(readOnly = true)
//    public IncidentResponse getById(Long id){
//        return IncidentResponse.from(getEntity(id));
//    }
    @Transactional
    public IncidentResponse resolve(Long id, ResolveRequest request) {
        Incident incident = getEntity(id);
        incident.setResolutionNotes(request.resolutionNotes());
        incident.setResolvedBy(request.resolvedBy());
        incident.setResolvedAt(LocalDateTime.now());
        incident.setStatus(IncidentStatus.RESOLVED);
        incident = incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        long total = incidentRepository.count();
        long open = incidentRepository.countByStatus(IncidentStatus.OPEN)
                + incidentRepository.countByStatus(IncidentStatus.ANALYZING)
                + incidentRepository.countByStatus(IncidentStatus.ANALYZED);
        long resolved = incidentRepository.countByStatus(IncidentStatus.RESOLVED);

        List<Incident> all = incidentRepository.findAll();
        long critical = all.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity())).count();

        var byCategory = all.stream()
                .filter(i -> i.getCategory() != null)
                .collect(java.util.stream.Collectors.groupingBy(Incident::getCategory, java.util.stream.Collectors.counting()));

        var bySeverity = all.stream()
                .filter(i -> i.getSeverity() != null)
                .collect(java.util.stream.Collectors.groupingBy(Incident::getSeverity, java.util.stream.Collectors.counting()));

        return new DashboardResponse(total, open, resolved, critical, byCategory, bySeverity);
    }

    private Incident getEntity(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
    }

}

