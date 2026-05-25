package com.example.ai.service.rag;

import com.example.ai.model.rag.RagEvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RagEvaluation {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluation.class);

    public RagEvaluationResult evaluate(
            String question,
            String[] expectedKeywords,
            List<String> retrievedChunkTexts,
            double[] similarityScores,
            String answer
    ) {
        Set<String> found = new HashSet<>();
        for (String chunk : retrievedChunkTexts) {
            for (String kw : expectedKeywords) {
                if (chunk.contains(kw)) {
                    found.add(kw);
                }
            }
        }

        int hitCount = found.size();
        double avgSim = similarityScores.length > 0
            ? Arrays.stream(similarityScores).average().orElse(0.0)
            : 0.0;

        boolean answerContains = false;
        for (String kw : expectedKeywords) {
            if (answer.contains(kw)) {
                answerContains = true;
                break;
            }
        }

        Set<String> missing = new HashSet<>(Arrays.asList(expectedKeywords));
        missing.removeAll(found);

        log.info("[RAG-EVAL] ═══════════════════════════════════");
        log.info("[RAG-EVAL] Question: \"{}\"", question);
        log.info("[RAG-EVAL] Expected keywords: {}", Arrays.toString(expectedKeywords));
        log.info("[RAG-EVAL] ───────────────────────────────────");
        log.info("[RAG-EVAL] Retrieval:");
        log.info("[RAG-EVAL]   - Hit count: {} / {} expected keywords found in chunks",
            hitCount, expectedKeywords.length);
        log.info("[RAG-EVAL]   - Avg similarity: {}", String.format("%.2f", avgSim));
        log.info("[RAG-EVAL] Answer:");
        log.info("[RAG-EVAL]   - Contains keywords: {} ({})",
            answerContains ? "YES" : "NO",
            found.isEmpty() ? "none found" : String.join(", ", found));
        if (!missing.isEmpty()) {
            log.info("[RAG-EVAL]   - Missing keywords: {}", missing);
        }
        log.info("[RAG-EVAL] ═══════════════════════════════════");

        return new RagEvaluationResult(
            question,
            expectedKeywords,
            found.toArray(new String[0]),
            hitCount,
            avgSim,
            answerContains
        );
    }

    public static void main(String[] args) {
        RagEvaluation eval = new RagEvaluation();
        List<String> chunks = List.of(
            "Redis支持String、List、Hash数据类型",
            "Redis还支持Set和ZSet有序集合",
            "MySQL是最流行的关系数据库"
        );
        double[] scores = {0.89, 0.85, 0.72};
        String answer = "Redis支持String、List、Hash、Set和ZSet五种数据结构。";

        eval.evaluate(
            "Redis 支持哪些数据结构？",
            new String[]{"String", "List", "Hash", "Set", "ZSet"},
            chunks,
            scores,
            answer
        );
    }
}