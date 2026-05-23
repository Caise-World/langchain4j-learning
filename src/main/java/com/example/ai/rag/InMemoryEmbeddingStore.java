package com.example.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEmbeddingStore {

    private final List<EmbeddedChunk> chunks = new ArrayList<>();
    private final Map<String, float[]> fakeVectorCache = new HashMap<>();

    public String add(Embedding embedding, TextSegment text) {
        String id = "chunk_" + System.nanoTime();
        chunks.add(new EmbeddedChunk(id, embedding, text));
        return id;
    }

    public List<String> search(Embedding queryEmbedding, int maxResults) {
        return findRelevantChunks(queryEmbedding, maxResults).stream()
                .map(c -> c.segment.text())
                .toList();
    }

    public List<EmbeddedChunk> findRelevantChunks(Embedding queryEmbedding, int maxResults) {
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, chunk.embedding)))
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(maxResults)
                .map(ScoredChunk::getChunk)
                .toList();
    }

    private double cosineSimilarity(Embedding a, Embedding b) {
        float[] vecA = a.vector();
        float[] vecB = b.vector();
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < vecA.length && i < vecB.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        return normA > 0 && normB > 0 ? dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)) : 0;
    }

    public float[] createSimpleEmbedding(String text) {
        if (fakeVectorCache.containsKey(text)) {
            return fakeVectorCache.get(text);
        }
        float[] vector = new float[1536];
        for (int i = 0; i < text.length() && i < 1536; i++) {
            vector[i] = (float) text.charAt(i) / 255.0f;
        }
        for (int i = text.length(); i < 1536; i++) {
            vector[i] = 0.1f;
        }
        fakeVectorCache.put(text, vector);
        return vector;
    }

    public void clear() {
        chunks.clear();
        fakeVectorCache.clear();
    }

    public int size() {
        return chunks.size();
    }

    public static class EmbeddedChunk {
        public final String id;
        public final Embedding embedding;
        public final TextSegment segment;

        public EmbeddedChunk(String id, Embedding embedding, TextSegment segment) {
            this.id = id;
            this.embedding = embedding;
            this.segment = segment;
        }
    }

    private static class ScoredChunk {
        final EmbeddedChunk chunk;
        final double score;

        ScoredChunk(EmbeddedChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        EmbeddedChunk getChunk() { return chunk; }
        double getScore() { return score; }
    }
}