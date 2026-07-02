package com.deveshparmar.codesage.scm.domain;

import com.deveshparmar.codesage.common.domain.Severity;

public record ReviewCommentRequest(
        String filePath,
        int startLine,
        int endLine,
        Severity severity,
        String message,
        String suggestion
) {
}
