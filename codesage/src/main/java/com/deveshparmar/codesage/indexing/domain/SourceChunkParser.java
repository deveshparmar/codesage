package com.deveshparmar.codesage.indexing.domain;

import java.util.UUID;

public interface SourceChunkParser {

    ParsedSourceFile parse(
            String relativePath,
            String sourceCode,
            UUID repositoryId,
            String branchName,
            String commitSha
    );

    boolean supports(String relativePath);
}
