package com.deveshparmar.codesage.indexing.domain;

import java.util.UUID;

public interface RepositoryClonePort {

    ClonedRepository cloneOrUpdate(
            UUID repositoryId,
            String cloneUrl,
            String accessToken,
            String branchName,
            String commitSha,
            boolean fullReindex
    );
}
