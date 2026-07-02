package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.review.domain.ModifiedMethod;
import org.springframework.stereotype.Component;

@Component
public class ReviewPromptBuilder {

    public String buildUserPrompt(
            String pullRequestTitle,
            ModifiedMethod method,
            String relatedContext
    ) {
        return """
                ## Pull Request
                Title: %s
                
                ## Changed Code Unit
                File: %s
                Class: %s
                Method: %s
                Lines: %d-%d
                Type: %s
                
                ### Source Code
                ```java
                %s
                ```
                
                ### Diff Patch
                ```diff
                %s
                ```
                
                ## Related Repository Context
                %s
                
                Review the changed code unit above. Focus on bugs, security issues, performance problems, and maintainability.
                Provide line numbers relative to the file shown above.
                """.formatted(
                pullRequestTitle,
                method.filePath(),
                method.className(),
                method.methodName() != null ? method.methodName() : "(type-level change)",
                method.startLine(),
                method.endLine(),
                method.chunkType(),
                method.sourceCode(),
                method.patch() != null ? method.patch() : "",
                relatedContext
        );
    }
}
