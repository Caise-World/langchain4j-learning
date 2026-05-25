package com.example.ai.service.rag;

import com.example.ai.model.rag.RagEvaluationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RagEvaluationTest {

    @Test
    void testEvaluate_withMatchingKeywords() {
        RagEvaluation eval = new RagEvaluation();
        List<String> chunks = List.of(
            "Redis支持String、List、Hash数据类型",
            "Redis还支持Set和ZSet有序集合"
        );
        double[] scores = {0.89, 0.85};
        String answer = "Redis支持String、List、Hash、Set和ZSet。";

        RagEvaluationResult result = eval.evaluate(
            "Redis 支持哪些数据结构？",
            new String[]{"String", "List", "Hash", "Set", "ZSet"},
            chunks,
            scores,
            answer
        );

        assertEquals(5, result.retrievalHitCount());
        assertEquals(0.87, result.averageSimilarity(), 0.01);
        assertTrue(result.answerContainsKeywords());
    }

    @Test
    void testEvaluate_withPartialMatch() {
        RagEvaluation eval = new RagEvaluation();
        List<String> chunks = List.of(
            "Redis支持String和List类型"
        );
        double[] scores = {0.82};
        String answer = "Redis是一种内存数据库。";

        RagEvaluationResult result = eval.evaluate(
            "Redis 支持哪些数据类型？",
            new String[]{"String", "List", "Hash"},
            chunks,
            scores,
            answer
        );

        assertEquals(2, result.retrievalHitCount());
        assertFalse(result.answerContainsKeywords());
    }
}