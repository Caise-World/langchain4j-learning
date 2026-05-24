package com.example.ai.infrastructure.embedding;

import dev.langchain4j.data.embedding.Embedding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEmbeddingStore implements EmbeddingStore {

    private final List<EmbeddedChunk> chunks = new ArrayList<>();
    private final Map<String, float[]> fakeVectorCache = new HashMap<>();

    @Override
    public String add(Embedding embedding, String text) {
        String id = "chunk_" + System.nanoTime();
        chunks.add(new EmbeddedChunk(id, embedding, dev.langchain4j.data.segment.TextSegment.from(text)));
        return id;
    }

    @Override
    public List<EmbeddedChunk> findRelevant(String text, int topK) {
        Embedding queryEmbedding = createEmbedding(text);
        return findRelevantChunks(queryEmbedding, topK);
    }

    public List<EmbeddedChunk> findRelevantChunks(Embedding queryEmbedding, int maxResults) {
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, chunk.embedding)))
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(maxResults)
                .map(ScoredChunk::getChunk)
                .toList();
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

    @Override
    public Embedding createEmbedding(String text) {
        return new Embedding(createSimpleEmbedding(text));
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

    @Override
    public void clear() {
        chunks.clear();
        fakeVectorCache.clear();
    }

    @Override
    public int size() {
        return chunks.size();
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