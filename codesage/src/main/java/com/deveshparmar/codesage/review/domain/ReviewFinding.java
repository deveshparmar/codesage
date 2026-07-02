package com.deveshparmar.codesage.review.domain;

import com.deveshparmar.codesage.common.domain.Severity;

public record ReviewFinding(
        String filePath,
        int startLine,
        int endLine,
        Severity severity,
        String category,
        String message,
        String suggestion,
        double rankScore
) {
    public ReviewFinding withRankScore(double score) {
        return new ReviewFinding(filePath, startLine, endLine, severity, category, message, suggestion, score);
    }
}
