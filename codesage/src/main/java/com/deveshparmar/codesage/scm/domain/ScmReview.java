package com.deveshparmar.codesage.scm.domain;

import java.time.Instant;

public record ScmReview(
        String externalId,
        String body,
        String state,
        Instant submittedAt
) {
}
