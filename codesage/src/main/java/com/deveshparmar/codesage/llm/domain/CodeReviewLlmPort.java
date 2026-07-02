package com.deveshparmar.codesage.llm.domain;

import com.deveshparmar.codesage.review.domain.LlmReviewResponse;

public interface CodeReviewLlmPort {

    LlmReviewResponse reviewCode(String systemPrompt, String userPrompt);
}
