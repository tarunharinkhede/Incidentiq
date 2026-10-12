package com.example.Incidentiq.service;

import com.example.Incidentiq.entity.Incident;
import com.example.Incidentiq.dto.SimilarIncidentMatch;
import com.example.Incidentiq.entity.IncidentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.Incidentiq.repository.IncidentRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SimilarityService {

    private final IncidentRepository incidentRepository;
    @Value("${incidentiq.similarity.threshold:0.45}")
    private double similarityThreshold;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "this", "that", "was", "were", "has", "have",
            "had", "not", "but", "are", "you", "your", "our", "into", "onto", "over", "under",
            "app", "application", "please", "info", "debug", "warn", "trace"

    );
    private static  final Pattern QUALIFIED_NAME = Pattern.compile("([a-zA-Z0-9_]+\\.)+[A-Za-z0-9_$]+");
    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z0-9_]{2,}");

    public SimilarityService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public Optional<SimilarIncidentMatch> findBestmatch( String newLog, Long excludeIncidentId){
        List<Incident> candidates = incidentRepository.findByStatusIn(
                List.of(IncidentStatus.ANALYZED, IncidentStatus.RESOLVED));

        Set<String> newTokens = tokenize(newLog);
        if (newTokens.isEmpty()) {
            return Optional.empty();
        }

        Incident bestIncident = null;
        double bestScore = 0.0;

        for (Incident candidate : candidates) {
            if (excludeIncidentId != null && excludeIncidentId.equals(candidate.getId())) {
                continue;
            }
            Set<String> candidateTokens = tokenize(candidate.getLog());
            double score = jaccard(newTokens, candidateTokens);
            if (score > bestScore) {
                bestScore = score;
                bestIncident = candidate;
            }
        }

        if (bestIncident != null && bestScore >= similarityThreshold) {
            return Optional.of(new SimilarIncidentMatch(bestIncident, round(bestScore)));
        }
        return Optional.empty();
    }

    private Set<String> tokenize(String log) {
        if (log == null || log.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();

        // Fully qualified class/exception names carry the most signal - weight them by keeping as-is (lowercased).
        Matcher qualified = QUALIFIED_NAME.matcher(log);
        while (qualified.find()) {
            tokens.add(qualified.group().toLowerCase(Locale.ROOT));
        }

        // General words, minus stopwords and pure numbers.
        Matcher words = WORD.matcher(log);
        while (words.find()) {
            String w = words.group().toLowerCase(Locale.ROOT);
            if (!STOPWORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

}
