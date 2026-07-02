package com.deveshparmar.codesage.rag.domain;

import java.util.List;

public interface VectorSearchPort {

    List<RetrievedContext> search(SimilaritySearchQuery query);
}
