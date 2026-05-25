package com.example.ai.service.rag;

import com.example.ai.model.dto.RagRequest;
import com.example.ai.model.rag.BenchmarkResult;
import com.example.ai.model.rag.RagQueryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Component
public class RagBenchmarkExecutor {

    private static final Logger log = LoggerFactory.getLogger(RagBenchmarkExecutor.class);
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagBenchmarkExecutor(RagService ragService) {
        this.ragService = ragService;
    }

    public List<BenchmarkResult> runBenchmark(List<QuestionCase> cases) {
        List<BenchmarkResult> results = new ArrayList<>();

        log.info("[BENCHMARK] Starting RAG Benchmark with {} test cases", cases.size());
        System.out.println("\n========================================");
        System.out.println("   RAG BENCHMARK START");
        System.out.println("========================================\n");

        for (int i = 0; i < cases.size(); i++) {
            QuestionCase qCase = cases.get(i);
            System.out.printf("[%d/%d] %s%n", i + 1, cases.size(), qCase.question);
            log.info("[BENCHMARK] Running case {}/{}: {}", i + 1, cases.size(), qCase.question);

            try {
                RagRequest request = new RagRequest();
                request.setQuestion(qCase.question);

                RagQueryResult queryResult = ragService.queryWithMetadata(request);
                BenchmarkResult result = evaluateCase(qCase, queryResult);
                results.add(result);

                printResult(result);
            } catch (Exception e) {
                log.error("[BENCHMARK] Error processing case: {}", qCase.question, e);
                BenchmarkResult errorResult = new BenchmarkResult(
                    qCase.question,
                    qCase.expectedKeywords,
                    qCase.expectReject,
                    -1,
                    -1,
                    -1,
                    false,
                    false,
                    "ERROR: " + e.getMessage()
                );
                results.add(errorResult);
                System.out.printf("   [ERROR] %s%n", e.getMessage());
            }
            System.out.println();
        }

        printSummary(results);
        return results;
    }

    private BenchmarkResult evaluateCase(QuestionCase qCase, RagQueryResult queryResult) {
        int hitCount = 0;
        for (String kw : qCase.expectedKeywords) {
            if (queryResult.answer().contains(kw)) {
                hitCount++;
            }
        }

        int retrievalChunks = queryResult.retrievalChunkCount();
        boolean answerContainsKeywords = hitCount > 0;
        boolean hallucination = false;

        if (qCase.expectReject) {
            hallucination = answerContainsKeywords;
        } else {
            hallucination = false;
        }

        return new BenchmarkResult(
            qCase.question,
            qCase.expectedKeywords,
            qCase.expectReject,
            retrievalChunks,
            hitCount,
            queryResult.answer().length(),
            answerContainsKeywords,
            hallucination,
            queryResult.answer()
        );
    }

    private void printResult(BenchmarkResult result) {
        String verdict;
        if (result.expectReject()) {
            if (result.hallucination()) {
                verdict = "[FAIL] Should reject but didn't";
            } else {
                verdict = "[PASS] Correctly rejected";
            }
        } else {
            if (result.hallucination()) {
                verdict = "[FAIL] Hallucination detected";
            } else if (result.retrievalChunkCount() == 0) {
                verdict = "[FAIL] No retrieval chunks";
            } else {
                verdict = "[PASS] Answer valid";
            }
        }

        System.out.printf("   Verdict: %s%n", verdict);
        System.out.printf("   Retrieval Chunks: %d%n", result.retrievalChunkCount());
        System.out.printf("   Hit Count: %d/%d%n", result.retrievalHitCount(), result.expectedKeywords().length);
        System.out.printf("   Answer Length: %d chars%n", result.answerLength());

        if (result.answer().length() > 0) {
            String preview = result.answer().length() > 150
                ? result.answer().substring(0, 150) + "..."
                : result.answer();
            System.out.printf("   Answer: %s%n", preview);
        }
    }

    private void printSummary(List<BenchmarkResult> results) {
        int total = results.size();
        int pass = 0;
        int fail = 0;
        int error = 0;
        int hallucination = 0;
        int noRetrieval = 0;

        for (BenchmarkResult r : results) {
            if (r.retrievalChunkCount() < 0) {
                error++;
            } else if (r.expectReject() && !r.hallucination()) {
                pass++;
            } else if (!r.expectReject() && !r.hallucination() && r.retrievalChunkCount() > 0) {
                pass++;
            } else {
                fail++;
                if (r.hallucination()) hallucination++;
                if (r.retrievalChunkCount() == 0) noRetrieval++;
            }
        }

        double passRate = total > 0 ? (double) pass / total * 100 : 0;

        System.out.println("========================================");
        System.out.println("   BENCHMARK SUMMARY");
        System.out.println("========================================");
        System.out.printf("   Total: %d | Pass: %d | Fail: %d | Error: %d%n", total, pass, fail, error);
        System.out.printf("   Pass Rate: %.1f%%%n", passRate);
        System.out.printf("   Hallucination Cases: %d%n", hallucination);
        System.out.printf("   No Retrieval Cases: %d%n", noRetrieval);
        System.out.println("========================================\n");
    }

    public void saveToJson(List<BenchmarkResult> results, String filePath) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", java.time.Instant.now().toString());
        report.put("totalCases", results.size());

        int pass = 0;
        int fail = 0;
        int hallucination = 0;
        int noRetrieval = 0;
        for (BenchmarkResult r : results) {
            if (r.retrievalChunkCount() < 0) {
                continue;
            } else if (r.expectReject() && !r.hallucination()) {
                pass++;
            } else if (!r.expectReject() && !r.hallucination() && r.retrievalChunkCount() > 0) {
                pass++;
            } else {
                fail++;
                if (r.hallucination()) hallucination++;
                if (r.retrievalChunkCount() == 0) noRetrieval++;
            }
        }
        report.put("passCount", pass);
        report.put("failCount", fail);
        report.put("hallucinationCount", hallucination);
        report.put("noRetrievalCount", noRetrieval);
        report.put("passRate", String.format("%.2f%%", results.isEmpty() ? 0 : (double) pass / results.size() * 100));

        report.put("results", results);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), report);

        log.info("[BENCHMARK] Report saved to {}", filePath);
    }

    public record QuestionCase(
        String question,
        String[] expectedKeywords,
        boolean expectReject
    ) {}
}