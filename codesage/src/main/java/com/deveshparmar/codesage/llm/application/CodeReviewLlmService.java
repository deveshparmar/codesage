package com.deveshparmar.codesage.llm.application;

import com.deveshparmar.codesage.llm.domain.CodeReviewLlmPort;
import com.deveshparmar.codesage.review.domain.LlmReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodeReviewLlmService {

    private final CodeReviewLlmPort codeReviewLlmPort;

    public LlmReviewResponse review(String systemPrompt, String userPrompt) {
        return codeReviewLlmPort.reviewCode(systemPrompt, userPrompt);
    }
}
