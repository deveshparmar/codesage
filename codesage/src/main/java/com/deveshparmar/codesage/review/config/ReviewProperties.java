package com.deveshparmar.codesage.review.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.review")
public class ReviewProperties {

    private int maxMethodsPerReview = 20;
    private int contextTopK = 5;
    private int maxFindingsPerMethod = 5;
    private double deduplicationLineOverlapThreshold = 0.5;
}
