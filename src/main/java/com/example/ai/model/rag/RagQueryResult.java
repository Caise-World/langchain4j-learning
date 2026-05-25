package com.example.ai.model.rag;

public record RagQueryResult(
    String answer,
    int retrievalChunkCount,
    int rerankChunkCount,
    int contextLength,
    int promptLength
) {}