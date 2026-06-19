# RAG Observability & Evaluation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add structured logging and lightweight evaluation to existing RAG service without modifying core logic or introducing complex observability infrastructure.

**Architecture:** Simple Spring components using slf4j for logging. RagTracer logs structured per-stage data at INFO/DEBUG levels. RagEvaluation provides console-based quality checks against expected keywords. No tracing frameworks, no metrics systems.

**Tech Stack:** slf4j (Spring Boot managed), Java record, JUnit 5

---

## File Structure

```
src/main/java/com/example/ai/
├── service/rag/
│   ├── RagService.java          (modify - inject RagTracer)
│   ├── RagTracer.java           (create - structured logger)
│   └── RagEvaluation.java      (create - lightweight evaluator)
└── model/rag/
    └── RagEvaluationResult.java (create - result record)

pom.xml                         (modify - add slf4j dep if needed)
```

---

## Task 1: Add slf4j dependency to pom.xml

**Files:**
- Modify: `pom.xml`

Spring Boot starter already includes slf4j, but verify the explicit API dependency is present.

- [ ] **Step 1: Read pom.xml and check existing slf4j dependency**

```bash
grep -A2 "slf4j" pom.xml
```

Expected: Should find `slf4j-api` under Spring Boot starter or explicit dep.

- [ ] **Step 2: If missing, add slf4j-api dependency**

Add if not present:
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml && git commit -m "chore: ensure slf4j-api dependency present"
```

---

## Task 2: Create RagTracer.java

**Files:**
- Create: `src/main/java/com/example/ai/service/rag/RagTracer.java`
- Test: none (logger only, manual verification via logs)

- [ ] **Step 1: Create RagTracer.java**

```java
package com.example.ai.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public void logRetrieval(int chunkCount, double avgSimilarity) {
        log.info("[RAG] Retrieval: found {} chunks, avg_similarity={}", chunkCount, String.format("%.2f", avgSimilarity));
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/ai/service/rag/RagTracer.java
git commit -m "feat: add RagTracer component for structured RAG logging"
```

---

## Task 3: Create RagEvaluationResult.java

**Files:**
- Create: `src/main/java/com/example/ai/model/rag/RagEvaluationResult.java`

- [ ] **Step 1: Create the record**

```java
package com.example.ai.model.rag;

public record RagEvaluationResult(
    String question,
    String[] expectedKeywords,
    String[] foundKeywords,
    int retrievalHitCount,
    double averageSimilarity,
    boolean answerContainsKeywords
) {}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/ai/model/rag/RagEvaluationResult.java
git commit -m "feat: add RagEvaluationResult record"
```

---

## Task 4: Create RagEvaluation.java

**Files:**
- Create: `src/main/java/com/example/ai/service/rag/RagEvaluation.java`
- Test: `src/test/java/com/example/ai/service/rag/RagEvaluationTest.java`

- [ ] **Step 1: Create RagEvaluation.java**

```java
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
```

- [ ] **Step 2: Create test class**

```java
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
```

- [ ] **Step 3: Run tests to verify**

```bash
mvn test -Dtest=RagEvaluationTest -q
```

Expected: PASS (2 tests)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/ai/service/rag/RagEvaluation.java
git add src/test/java/com/example/ai/service/rag/RagEvaluationTest.java
git commit -m "feat: add RagEvaluation lightweight evaluator"
```

---

## Task 5: Modify RagService.java to integrate RagTracer

**Files:**
- Modify: `src/main/java/com/example/ai/service/rag/RagService.java`

- [ ] **Step 1: Read RagService.java to understand current structure**

Focus on:
- Current imports
- How to inject RagTracer (add field + constructor injection)
- Where each method (query, rerankAndFilter, generateMultipleQueries) is located

- [ ] **Step 2: Add imports and field**

Add imports:
```java
import com.example.ai.service.rag.RagTracer;
import com.example.ai.service.rag.RagTracer.RagChunkInfo;
import com.example.ai.service.rag.RagTracer.RerankEntry;
```

Add field after class declaration:
```java
private final RagTracer tracer;
```

Add constructor parameter:
```java
public RagService(..., RagTracer tracer) {
    ...
    this.tracer = tracer;
}
```

- [ ] **Step 3: Add tracing to generateMultipleQueries()**

At end of method (after parsing queries):
```java
tracer.logMultiQuery(expandedQueries.size());
```

- [ ] **Step 4: Add tracing to query() method**

After retrieval loop (before rerankAndFilter):
```java
List<RagChunkInfo> chunkInfos = chunks.stream()
    .map(c -> new RagChunkInfo(c.getTextSegment().getText(), c.getScore()))
    .toList();
tracer.logRetrieval(chunkInfos.size(), avgSimilarity);
tracer.logRetrievalDetails(chunkInfos);
```

After rerankAndFilter call:
```java
tracer.logRerank(candidates.size(), rankedAndFiltered.size(), RERANK_SCORE_THRESHOLD);
List<RerankEntry> beforeEntries = candidates.stream()
    .map(c -> new RerankEntry(c.getTextSegment().getText(), c.getScore()))
    .toList();
List<RerankEntry> afterEntries = rankedAndFiltered.stream()
    .map(c -> new RerankEntry(c.getTextSegment().getText(), c.getScore()))
    .toList();
tracer.logRerankDetails(beforeEntries, afterEntries);
```

Before return (building final context):
```java
tracer.logFinalContext(context.length(), prompt.length());
tracer.logAnswer(answer);
```

- [ ] **Step 5: Add query tracing at method start**

First line of query() method:
```java
tracer.logQuery(userMessage);
```

- [ ] **Step 6: Run tests to verify no regression**

```bash
mvn test -q
```

Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/ai/service/rag/RagService.java
git commit -m "feat: integrate RagTracer into RagService for observability"
```

---

## Task 6: Verify logging works via manual test

**Files:**
- None (verification only)

- [ ] **Step 1: Start application and call RAG endpoint**

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Redis 支持哪些数据结构？"}'
```

- [ ] **Step 2: Check log output**

Verify INFO logs appear:
```
[RAG] Query: "Redis 支持哪些数据结构？"
[RAG] Multi-query expanded: 3 queries generated
[RAG] Retrieval: found X chunks, avg_similarity=0.XX
[RAG] Rerank: X → Y chunks (score >= 5.0)
[RAG] Final context: XXX chars, prompt: XXX chars
[RAG] Answer generated (len=XXX)
```

Verify DEBUG logs (if enabled) show chunk details with truncated text.

---

## Verification Checklist

- [ ] pom.xml has slf4j-api dependency
- [ ] RagTracer.java compiles and is a Spring Component
- [ ] RagEvaluation.java can be run via main() method
- [ ] RagEvaluationTest passes (2 tests)
- [ ] RagService.java compiles with RagTracer injection
- [ ] All existing tests pass
- [ ] INFO logs appear on RAG query
- [ ] DEBUG logs show chunk details (if log level is DEBUG)
- [ ] No changes to EmbeddingStore interface
- [ ] No changes to multi-query/rerank core logic

---

## Execution Options

**1. Subagent-Driven (recommended)** - Dispatch fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?