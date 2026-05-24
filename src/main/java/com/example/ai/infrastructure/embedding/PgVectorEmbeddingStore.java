package com.example.ai.infrastructure.embedding;

import dev.langchain4j.data.embedding.Embedding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PgVectorEmbeddingStore implements EmbeddingStore {

    private final EntityManager em;
    private static final int DIMENSION = 1536;

    public PgVectorEmbeddingStore(EntityManager em) {
        this.em = em;
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
        Embedding queryEmbedding = createEmbedding(text);
        float[] queryVector = queryEmbedding.vector();

        Query query = em.createNativeQuery(
                "SELECT id, embedding::text, text FROM embeddings ORDER BY embedding <=> ?::vector LIMIT ?"
        );
        try {
            query.setParameter(1, vectorToSqlArray(queryVector));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create query vector", e);
        }
        query.setParameter(2, topK);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (Object[] row : rows) {
            String id = (String) row[0];
            String embeddingText = (String) row[1];
            String content = (String) row[2];

            float[] floatVector = parseVector(embeddingText);
            chunks.add(new EmbeddedChunk(
                    id,
                    new Embedding(floatVector),
                    dev.langchain4j.data.segment.TextSegment.from(content)
            ));
        }

        return chunks;
    }

    @Override
    public Embedding createEmbedding(String text) {
        float[] vector = new float[DIMENSION];
        for (int i = 0; i < text.length() && i < DIMENSION; i++) {
            vector[i] = (float) text.charAt(i) / 255.0f;
        }
        for (int i = text.length(); i < DIMENSION; i++) {
            vector[i] = 0.1f;
        }
        return new Embedding(vector);
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