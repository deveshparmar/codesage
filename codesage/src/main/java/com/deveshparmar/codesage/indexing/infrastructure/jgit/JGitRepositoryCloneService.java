package com.deveshparmar.codesage.indexing.infrastructure.jgit;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import com.deveshparmar.codesage.indexing.domain.ClonedRepository;
import com.deveshparmar.codesage.indexing.domain.RepositoryClonePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JGitRepositoryCloneService implements RepositoryClonePort {

    private final IndexingProperties indexingProperties;

    @Override
    public ClonedRepository cloneOrUpdate(
            UUID repositoryId,
            String cloneUrl,
            String accessToken,
            String branchName,
            String commitSha,
            boolean fullReindex
    ) {
        Path localPath = Path.of(indexingProperties.getWorkspacePath(), repositoryId.toString());
        UsernamePasswordCredentialsProvider credentials = credentialsProvider(accessToken);

        try {
            if (fullReindex && Files.exists(localPath)) {
                deleteRecursively(localPath);
            }

            if (!Files.exists(localPath.resolve(".git"))) {
                Files.createDirectories(localPath.getParent());
                log.info("Cloning repository {} into {}", repositoryId, localPath);
                Git.cloneRepository()
                        .setURI(cloneUrl)
                        .setDirectory(localPath.toFile())
                        .setBranch(branchName)
                        .setCloneAllBranches(false)
                        .setCredentialsProvider(credentials)
                        .call()
                        .close();
            } else {
                log.info("Updating existing clone for repository {}", repositoryId);
                try (Git git = openGit(localPath)) {
                    git.fetch()
                            .setCredentialsProvider(credentials)
                            .call();
                }
            }

            String resolvedCommitSha = commitSha;
            try (Git git = openGit(localPath)) {
                git.checkout()
                        .setName(branchName)
                        .setForced(true)
                        .call();

                if (resolvedCommitSha == null || resolvedCommitSha.isBlank()) {
                    resolvedCommitSha = git.getRepository().resolve("HEAD").getName();
                }

                ObjectId resolvedCommit = git.getRepository().resolve(resolvedCommitSha);
                if (resolvedCommit == null) {
                    throw new CodeSageException("Commit not found: " + resolvedCommitSha);
                }
                git.checkout()
                        .setName(resolvedCommitSha)
                        .setForced(true)
                        .call();
            }

            return new ClonedRepository(repositoryId, localPath, branchName, resolvedCommitSha);
        } catch (Exception ex) {
            throw new CodeSageException("Failed to clone or update repository " + repositoryId, ex);
        }
    }

    private Git openGit(Path localPath) throws IOException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(localPath.resolve(".git").toFile())
                .build();
        return new Git(repository);
    }

    private UsernamePasswordCredentialsProvider credentialsProvider(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider("token", accessToken);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        FileUtils.delete(path.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
    }
}
