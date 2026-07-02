package com.deveshparmar.codesage.review.domain;

import java.util.List;

public record LlmReviewResponse(
        String summary,
        List<LlmFinding> findings,
        int promptTokens,
        int completionTokens
) {
    public record LlmFinding(
            String severity,
            String category,
            String message,
            String suggestion,
            Integer startLine,
            Integer endLine
    ) {
    }
}
