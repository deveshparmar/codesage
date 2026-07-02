package com.deveshparmar.codesage.scm.domain;

import java.util.List;

public interface RepositoryProvider {

    ScmRepository fetchRepository(ScmAccessToken token, String owner, String repositoryName);

    List<ScmRepository> listRepositories(ScmAccessToken token, String owner);

    String getDefaultBranch(ScmAccessToken token, String owner, String repositoryName);
}
