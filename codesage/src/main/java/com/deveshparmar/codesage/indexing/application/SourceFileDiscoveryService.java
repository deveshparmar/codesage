package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import com.deveshparmar.codesage.indexing.domain.ClonedRepository;
import com.deveshparmar.codesage.indexing.domain.ParsedSourceFile;
import com.deveshparmar.codesage.indexing.domain.SourceChunkParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceFileDiscoveryService {

    private final IndexingProperties indexingProperties;
    private final List<SourceChunkParser> chunkParsers;

    public List<ParsedSourceFile> discoverAndParse(ClonedRepository clonedRepository) throws IOException {
        List<ParsedSourceFile> parsedFiles = new ArrayList<>();
        UUID repositoryId = clonedRepository.repositoryId();
        String branchName = clonedRepository.branchName();
        String commitSha = clonedRepository.commitSha();

        try (var paths = Files.walk(clonedRepository.localPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(path, clonedRepository.localPath()))
                    .forEach(path -> parseFile(path, clonedRepository.localPath(), repositoryId, branchName, commitSha, parsedFiles));
        }
        return parsedFiles;
    }

    private void parseFile(
            Path absolutePath,
            Path repositoryRoot,
            UUID repositoryId,
            String branchName,
            String commitSha,
            List<ParsedSourceFile> parsedFiles
    ) {
        String relativePath = repositoryRoot.relativize(absolutePath).toString().replace('\\', '/');
        SourceChunkParser parser = chunkParsers.stream()
                .filter(candidate -> candidate.supports(relativePath))
                .findFirst()
                .orElse(null);
        if (parser == null) {
            return;
        }
        try {
            String sourceCode = Files.readString(absolutePath);
            ParsedSourceFile parsed = parser.parse(relativePath, sourceCode, repositoryId, branchName, commitSha);
            if (!parsed.chunks().isEmpty()) {
                parsedFiles.add(parsed);
            }
        } catch (IOException ex) {
            // Skip unreadable files during indexing.
        }
    }

    private boolean isExcluded(Path path, Path repositoryRoot) {
        Path relative = repositoryRoot.relativize(path);
        for (String excluded : indexingProperties.getExcludedDirectories()) {
            for (Path part : relative) {
                if (part.toString().equals(excluded)) {
                    return true;
                }
            }
        }
        return false;
    }
}
