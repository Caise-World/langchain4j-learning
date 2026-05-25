package com.example.ai.controller;

import com.example.ai.model.rag.BenchmarkResult;
import com.example.ai.service.rag.RagBenchmarkExecutor;
import com.example.ai.service.rag.RagBenchmarkExecutor.QuestionCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/rag/benchmark")
@RequiredArgsConstructor
public class RagBenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(RagBenchmarkController.class);
    private final RagBenchmarkExecutor benchmarkExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/run")
    public String runBenchmark() throws Exception {
        log.info("[BENCHMARK] Triggered benchmark via API");

        List<QuestionCase> cases = loadCasesFromJson();
        List<BenchmarkResult> results = benchmarkExecutor.runBenchmark(cases);

        Path reportPath = Paths.get("rag-benchmark-report.json");
        benchmarkExecutor.saveToJson(results, reportPath.toString());

        return "Benchmark completed. Report saved to " + reportPath.toAbsolutePath();
    }

    private List<QuestionCase> loadCasesFromJson() throws Exception {
        ClassPathResource resource = new ClassPathResource("rag-test-questions.json");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(content);

        List<QuestionCase> cases = new ArrayList<>();
        for (JsonNode node : root) {
            String question = node.get("question").asText();
            JsonNode kwArray = node.get("expected_keywords");
            String[] keywords = new String[kwArray.size()];
            for (int i = 0; i < kwArray.size(); i++) {
                keywords[i] = kwArray.get(i).asText();
            }
            boolean expectReject = node.get("expect_reject").asBoolean();

            cases.add(new QuestionCase(question, keywords, expectReject));
        }
        return cases;
    }
}