package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.common.domain.Severity;
import com.deveshparmar.codesage.review.domain.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class FindingRanker {

    public List<ReviewFinding> rank(List<ReviewFinding> findings) {
        return findings.stream()
                .map(this::assignScore)
                .sorted(Comparator.comparingDouble(ReviewFinding::rankScore).reversed())
                .toList();
    }

    private ReviewFinding assignScore(ReviewFinding finding) {
        double severityScore = switch (finding.severity()) {
            case ERROR -> 100.0;
            case WARNING -> 60.0;
            case INFO -> 20.0;
        };
        double categoryScore = switch (finding.category().toUpperCase()) {
            case "SECURITY" -> 30.0;
            case "BUG" -> 5.0;
            case "PERFORMANCE" -> 15.0;
            case "MAINTAINABILITY" -> 10.0;
            default -> 5.0;
        };
        return finding.withRankScore(severityScore + categoryScore);
    }
}
