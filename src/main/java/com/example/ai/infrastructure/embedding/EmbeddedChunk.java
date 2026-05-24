package com.example.ai.infrastructure.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public class EmbeddedChunk {
    public final String id;
    public final Embedding embedding;
    public final TextSegment segment;

    public EmbeddedChunk(String id, Embedding embedding, TextSegment segment) {
        this.id = id;
        this.embedding = embedding;
        this.segment = segment;
    }

    public String getText() {
        return segment.text();
    }
}