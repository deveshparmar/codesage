package com.deveshparmar.codesage.scm.domain;

import java.time.Instant;

public record ScmReviewComment(
        String externalId,
        String filePath,
        int line,
        String body,
        Instant createdAt
) {
}
