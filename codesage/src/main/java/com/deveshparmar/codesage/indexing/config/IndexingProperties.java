package com.deveshparmar.codesage.indexing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.indexing")
public class IndexingProperties {

    private String workspacePath = "/tmp/codesage/repos";
    private Duration lockTtl = Duration.ofMinutes(30);
    private String javaFileGlob = "**/*.java";
    private List<String> excludedDirectories = new ArrayList<>(List.of(
            ".git", "target", "build", "out", "node_modules", ".gradle"
    ));
    private int embeddingBatchSize = 20;
}
