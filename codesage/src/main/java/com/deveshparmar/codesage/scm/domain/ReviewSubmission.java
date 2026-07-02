package com.deveshparmar.codesage.scm.domain;

import java.time.Instant;
import java.util.List;

public record ReviewSubmission(
        String summary,
        List<ReviewCommentRequest> comments,
        ReviewEvent event
) {
    public enum ReviewEvent {
        COMMENT,
        APPROVE,
        REQUEST_CHANGES
    }
}
