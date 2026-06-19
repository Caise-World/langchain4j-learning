package com.example.ai.infrastructure.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PgVectorEmbeddingStore implements EmbeddingStore {

    private final EntityManager em;
    private static final int DIMENSION = 768; // nomic-embed-text uses 768 dimensions
    private static final String MODEL = "nomic-embed-text";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String ollamaUrl;

    public PgVectorEmbeddingStore(EntityManager em, @Value("${ollama.host:http://localhost:11434}") String ollamaHost) {
        this.em = em;
        this.ollamaUrl = ollamaHost + "/api/embeddings";
        initTable();
    }

    private void initTable() {
        em.getTransaction().begin();
        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS embeddings (
                id VARCHAR(64) PRIMARY KEY,
                embedding vector(%d) NOT NULL,
                text TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.formatted(DIMENSION)).executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public String add(Embedding embedding, String text) {
        String id = UUID.randomUUID().toString();
        float[] vector = embedding.vector();

        em.getTransaction().begin();
        try {
            em.createNativeQuery("INSERT INTO embeddings (id, embedding, text) VALUES (?, ?::vector, ?)")
                    .setParameter(1, id)
                    .setParameter(2, vectorToSqlArray(vector))
                    .setParameter(3, text)
                    .executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add embedding", e);
        }
        em.getTransaction().commit();

        return id;
    }

    @Override
    public List<EmbeddedChunk> findRelevant(String text, int topK) {
        return findRelevant(text, topK, 0.6);
    }

    public List<EmbeddedChunk> findRelevant(String text, int topK, double threshold) {
        return findRelevantWithScores(text, topK).stream()
                .filter(sc -> sc.getScore() >= threshold)
                .map(InMemoryEmbeddingStore.ScoredChunk::getChunk)
                .toList();
    }

    @Override
    public List<InMemoryEmbeddingStore.ScoredChunk> findRelevantWithScores(String text, int topK) {
        Embedding queryEmbedding = createEmbedding(text);
        return findRelevantChunksWithScores(queryEmbedding, topK, 0.0);
    }

    public List<InMemoryEmbeddingStore.ScoredChunk> findRelevantChunksWithScores(Embedding queryEmbedding, int maxResults, double threshold) {
        float[] queryVector = queryEmbedding.vector();

        // PgVector <=> operator returns cosine distance (0 = identical, 2 = opposite)
        // Convert to similarity score: score = 1 - distance
        Query query = em.createNativeQuery(
                "SELECT id, embedding::text, text, (1 - (embedding <=> ?::vector)) as similarity FROM embeddings ORDER BY embedding <=> ?::vector LIMIT ?"
        );
        try {
            query.setParameter(1, vectorToSqlArray(queryVector));
            query.setParameter(2, vectorToSqlArray(queryVector));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create query vector", e);
        }
        query.setParameter(3, maxResults);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<InMemoryEmbeddingStore.ScoredChunk> results = new ArrayList<>();
        for (Object[] row : rows) {
            String id = (String) row[0];
            String embeddingText = (String) row[1];
            String content = (String) row[2];
            Double similarity = ((Number) row[3]).doubleValue();

            float[] floatVector = parseVector(embeddingText);
            EmbeddedChunk chunk = new EmbeddedChunk(
                    id,
                    new Embedding(floatVector),
                    dev.langchain4j.data.segment.TextSegment.from(content)
            );
            results.add(new InMemoryEmbeddingStore.ScoredChunk(chunk, similarity));
        }

        return results;
    }

    public List<EmbeddedChunk> findRelevantChunks(Embedding queryEmbedding, int maxResults, double threshold) {
        return findRelevantChunksWithScores(queryEmbedding, maxResults, threshold).stream()
                .filter(sc -> sc.getScore() >= threshold)
                .map(InMemoryEmbeddingStore.ScoredChunk::getChunk)
                .toList();
    }

    @Override
    public Embedding createEmbedding(String text) {
        try {
            Map<String, String> requestBody = Map.of("model", MODEL, "prompt", text);
            String json = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(ollamaUrl)
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
                return new Embedding(result);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch embedding from Ollama", e);
        }
    }

    @Override
    public void clear() {
        em.getTransaction().begin();
        em.createNativeQuery("TRUNCATE TABLE embeddings").executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public int size() {
        Long count = (Long) em.createNativeQuery("SELECT COUNT(*) FROM embeddings").getSingleResult();
        return count.intValue();
    }

    private Array vectorToSqlArray(float[] vector) throws SQLException {
        Object[] boxed = new Object[vector.length];
        for (int i = 0; i < vector.length; i++) {
            boxed[i] = vector[i];
        }
        return em.unwrap(java.sql.Connection.class).createArrayOf("real", boxed);
    }

    private float[] parseVector(String vectorText) {
        String[] parts = vectorText.replace("{", "").replace("}", "").split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}