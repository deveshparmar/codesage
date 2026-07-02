package com.deveshparmar.codesage.scm.domain;

public record ScmPullRequestFile(
        String filename,
        String status,
        int additions,
        int deletions,
        int changes,
        String patch
) {
}
