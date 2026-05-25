package com.example.ai.model.rag;

public record RagEvaluationResult(
    String question,
    String[] expectedKeywords,
    String[] foundKeywords,
    int retrievalHitCount,
    double averageSimilarity,
    boolean answerContainsKeywords
) {}