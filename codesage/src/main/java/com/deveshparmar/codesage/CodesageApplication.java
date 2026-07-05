package com.deveshparmar.codesage;

import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.config.OpenAiProperties;
import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import com.deveshparmar.codesage.rag.config.RagProperties;
import com.deveshparmar.codesage.review.config.ReviewProperties;
import com.deveshparmar.codesage.scm.config.GitHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        CodeSageProperties.class,
        GitHubProperties.class,
        IndexingProperties.class,
        EmbeddingProperties.class,
        OpenAiProperties.class,
        RagProperties.class,
        ReviewProperties.class
})
public class CodesageApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodesageApplication.class, args);
	}

}
