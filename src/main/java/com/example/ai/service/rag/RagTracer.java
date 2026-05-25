package com.example.ai.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagTracer {

    private static final Logger log = LoggerFactory.getLogger(RagTracer.class);
    private static final int MAX_TEXT_LEN = 200;

    public void logQuery(String query) {
        log.info("[RAG] Query: \"{}\"", query);
    }

    public void logMultiQuery(int count) {
        log.info("[RAG] Multi-query expanded: {} queries generated", count);
    }

    public void logRetrieval(int chunkCount) {
        log.info("[RAG] Retrieval: found {} chunks", chunkCount);
    }

    public void logRetrievalDetails(List<RagChunkInfo> chunks) {
        if (!log.isDebugEnabled()) return;
        log.debug("[RAG] ── Retrieval Details ──────────────────");
        for (int i = 0; i < chunks.size(); i++) {
            RagChunkInfo chunk = chunks.get(i);
            log.debug("[RAG]   [{}] score={} | \"{}\"",
                i + 1,
                String.format("%.2f", chunk.score()),
                truncateText(chunk.text())
            );
        }
        log.debug("[RAG] ── End ────────────────────────────────");
    }

    public void logRerank(int beforeCount, int afterCount, double threshold) {
        log.info("[RAG] Rerank: {} → {} chunks (score >= {})", beforeCount, afterCount, String.format("%.1f", threshold));
    }

    public void logRerankDetails(List<RerankEntry> before, List<RerankEntry> after) {
        if (!log.isDebugEnabled()) return;
        log.debug("[RAG] ── Rerank Details ────────────────────");
        log.debug("[RAG]   BEFORE: {} chunks", before.size());
        before.forEach(e -> log.debug("[RAG]     score={} | \"{}\"",
            String.format("%.2f", e.score()), truncateText(e.text())));
        log.debug("[RAG]   AFTER: {} chunks", after.size());
        after.forEach(e -> log.debug("[RAG]     score={} | \"{}\"",
            String.format("%.2f", e.score()), truncateText(e.text())));
        log.debug("[RAG] ── End ────────────────────────────────");
    }

    public void logFinalContext(int contextLen, int promptLen) {
        log.info("[RAG] Final context: {} chars, prompt: {} chars", contextLen, promptLen);
    }

    public void logAnswer(String answer) {
        log.info("[RAG] Answer generated (len={})", answer.length());
        if (log.isDebugEnabled()) {
            log.debug("[RAG] Answer content: \"{}\"", truncateText(answer));
        }
    }

    private String truncateText(String text) {
        if (text == null) return "null";
        if (text.length() <= MAX_TEXT_LEN) return text;
        return text.substring(0, MAX_TEXT_LEN) + "...";
    }

    public record RagChunkInfo(String text, double score) {}
    public record RerankEntry(String text, double score) {}
}