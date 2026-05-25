package com.example.ai.model.rag;

public record BenchmarkResult(
    String question,
    String[] expectedKeywords,
    boolean expectReject,
    int retrievalHitCount,
    int answerLength,
    boolean answerContainsExpectedKeywords,
    boolean hallucination,
    String answer
) {}