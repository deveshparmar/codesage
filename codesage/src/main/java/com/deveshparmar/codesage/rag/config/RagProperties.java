package com.deveshparmar.codesage.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.rag")
public class RagProperties {

    private int defaultTopK = 10;
    private double minSimilarity = 0.7;
}
