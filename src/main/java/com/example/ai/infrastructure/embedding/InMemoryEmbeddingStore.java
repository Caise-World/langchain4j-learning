package com.example.ai.infrastructure.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEmbeddingStore implements EmbeddingStore {

    private static final String OLLAMA_URL = "http://localhost:11434/api/embeddings";
    private static final String MODEL = "nomic-embed-text";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<EmbeddedChunk> chunks = new ArrayList<>();
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    @Override
    public String add(Embedding embedding, String text) {
        String id = "chunk_" + System.nanoTime();
        chunks.add(new EmbeddedChunk(id, embedding, dev.langchain4j.data.segment.TextSegment.from(text)));
        return id;
    }

    @Override
    public List<EmbeddedChunk> findRelevant(String text, int topK) {
        return findRelevant(text, topK, 0.65);
    }

    public List<EmbeddedChunk> findRelevant(String text, int topK, double threshold) {
        Embedding queryEmbedding = createEmbedding(text);
        return findRelevantChunks(queryEmbedding, topK, threshold);
    }

    public List<EmbeddedChunk> findRelevantChunks(Embedding queryEmbedding, int maxResults, double threshold) {
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, chunk.embedding)))
                .filter(sc -> sc.getScore() >= threshold)
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(maxResults)
                .map(ScoredChunk::getChunk)
                .toList();
    }

    private float[] fetchEmbedding(String text) {
        try {
            Map<String, String> requestBody = Map.of("model", MODEL, "prompt", text);
            String json = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(OLLAMA_URL)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Ollama API error: " + response.code() + " - " + response.body().string());
                }
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode embeddingNode = root.get("embedding");
                if (embeddingNode == null) {
                    throw new RuntimeException("No 'embedding' in Ollama response: " + responseBody);
                }
                float[] result = new float[embeddingNode.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (float) embeddingNode.get(i).asDouble();
                }
                return result;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch embedding from Ollama", e);
        }
    }

    @Override
    public Embedding createEmbedding(String text) {
        return new Embedding(cache.computeIfAbsent(text, this::fetchEmbedding));
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
        cache.clear();
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