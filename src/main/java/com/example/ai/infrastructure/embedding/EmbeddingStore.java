package com.example.ai.infrastructure.embedding;

import dev.langchain4j.data.embedding.Embedding;

import java.util.List;

public interface EmbeddingStore {
    String add(Embedding embedding, String text);
    List<EmbeddedChunk> findRelevant(String text, int topK);
    Embedding createEmbedding(String text);
    void clear();
    int size();
}