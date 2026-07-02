package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.review.config.ReviewProperties;
import com.deveshparmar.codesage.review.domain.ReviewFinding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FindingDeduplicator {

    private final ReviewProperties reviewProperties;

    public List<ReviewFinding> deduplicate(List<ReviewFinding> findings) {
        List<ReviewFinding> sorted = findings.stream()
                .sorted(Comparator.comparingDouble(ReviewFinding::rankScore).reversed())
                .toList();

        List<ReviewFinding> unique = new ArrayList<>();
        for (ReviewFinding candidate : sorted) {
            if (unique.stream().noneMatch(existing -> isDuplicate(existing, candidate))) {
                unique.add(candidate);
            }
        }
        return unique;
    }

    private boolean isDuplicate(ReviewFinding left, ReviewFinding right) {
        if (!left.filePath().equals(right.filePath())) {
            return false;
        }
        if (!left.category().equalsIgnoreCase(right.category())) {
            return false;
        }
        return lineOverlapRatio(left.startLine(), left.endLine(), right.startLine(), right.endLine())
                >= reviewProperties.getDeduplicationLineOverlapThreshold();
    }

    private double lineOverlapRatio(int startA, int endA, int startB, int endB) {
        int overlapStart = Math.max(startA, startB);
        int overlapEnd = Math.min(endA, endB);
        if (overlapEnd < overlapStart) {
            return 0.0;
        }
        int overlap = overlapEnd - overlapStart + 1;
        int spanA = endA - startA + 1;
        int spanB = endB - startB + 1;
        int minSpan = Math.min(spanA, spanB);
        return minSpan == 0 ? 0.0 : (double) overlap / minSpan;
    }
}
