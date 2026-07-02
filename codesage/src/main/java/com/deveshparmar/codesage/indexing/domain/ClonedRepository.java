package com.deveshparmar.codesage.indexing.domain;

import java.nio.file.Path;
import java.util.UUID;

public record ClonedRepository(
        UUID repositoryId,
        Path localPath,
        String branchName,
        String commitSha
) {
}
