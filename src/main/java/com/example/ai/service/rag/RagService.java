package com.example.ai.service.rag;

import com.example.ai.config.PromptProperties;
import com.example.ai.config.PromptTemplateLoader;
import com.example.ai.infrastructure.embedding.EmbeddingStore;
import com.example.ai.infrastructure.embedding.EmbeddedChunk;
import com.example.ai.infrastructure.embedding.InMemoryEmbeddingStore;
import com.example.ai.model.dto.RagRequest;
import com.example.ai.model.rag.RagQueryResult;
import com.example.ai.rag.*;
import com.example.ai.service.llm.ModelFactory;
import com.example.ai.service.rag.RagTracer;
import com.example.ai.service.rag.RagTracer.RagChunkInfo;
import com.example.ai.service.rag.RagTracer.RerankEntry;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ModelFactory modelFactory;
    private final TextSplitter textSplitter;
    private final EmbeddingStore embeddingStore;
    private final RagTracer tracer;
    private final PromptProperties promptProperties;
    private final PromptTemplateLoader promptLoader;

    public int ingestDocument(String filePath) throws Exception {
        DocumentLoader loader = new DocumentLoader();
        Document doc = loader.load(filePath);
        List<TextSplitter.TextChunk> chunks = textSplitter.split(doc.getContent(), doc.getId());

        for (TextSplitter.TextChunk chunk : chunks) {
            dev.langchain4j.data.embedding.Embedding embedding =
                    embeddingStore.createEmbedding(chunk.getContent());
            embeddingStore.add(embedding, chunk.getContent());
        }

        return chunks.size();
    }

    public String query(RagRequest request) {
        String originalQuestion = request.getQuestion();
        tracer.logQuery(originalQuestion);
        List<String> queries = generateMultipleQueries(originalQuestion);
        tracer.logMultiQuery(queries.size());

        int totalChunks = embeddingStore.size();
        int topK = calculateDynamicTopK(totalChunks, request.getTopK());

        Set<String> seenTexts = new HashSet<>();
        List<EmbeddedChunk> mergedChunks = new ArrayList<>();
        for (String q : queries) {
            List<EmbeddedChunk> chunks = embeddingStore.findRelevant(q, topK * 3);
            if (chunks != null) {
                for (EmbeddedChunk chunk : chunks) {
                    String text = chunk.segment.text();
                    if (!seenTexts.contains(text)) {
                        seenTexts.add(text);
                        mergedChunks.add(chunk);
                    }
                }
            }
        }

        List<RagChunkInfo> chunkInfos = mergedChunks.stream()
            .map(c -> new RagChunkInfo(c.segment.text(), 0.0))
            .toList();
        tracer.logRetrieval(chunkInfos.size());
        tracer.logRetrievalDetails(chunkInfos);

        List<EmbeddedChunk> rerankedChunks = rerankAndFilter(originalQuestion, mergedChunks, topK);

        List<RerankEntry> beforeEntries = mergedChunks.stream()
            .map(c -> new RerankEntry(c.segment.text(), 0.0))
            .toList();
        List<RerankEntry> afterEntries = rerankedChunks.stream()
            .map(c -> new RerankEntry(c.segment.text(), 0.0))
            .toList();
        tracer.logRerank(mergedChunks.size(), rerankedChunks.size(), 5.0);
        tracer.logRerankDetails(beforeEntries, afterEntries);

        StringBuilder context = new StringBuilder();
        int index = 1;
        for (EmbeddedChunk chunk : rerankedChunks) {
            String text = chunk.segment.text();
            if (text.length() < 50) continue;
            context.append("[").append(index).append("] ").append(text).append("\n\n");
            index++;
        }

        String prompt = buildPrompt(originalQuestion, context.toString());

        RagAssistant assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String answer = assistant.chat(prompt);
        tracer.logFinalContext(context.length(), prompt.length());
        tracer.logAnswer(answer);
        return answer;
    }

    public RagQueryResult queryWithMetadata(RagRequest request) {
        String originalQuestion = request.getQuestion();
        tracer.logQuery(originalQuestion);
        List<String> queries = generateMultipleQueries(originalQuestion);
        tracer.logMultiQuery(queries.size());

        int totalChunks = embeddingStore.size();
        int topK = calculateDynamicTopK(totalChunks, request.getTopK());

        Set<String> seenTexts = new HashSet<>();
        List<InMemoryEmbeddingStore.ScoredChunk> mergedScoredChunks = new ArrayList<>();
        for (String q : queries) {
            List<InMemoryEmbeddingStore.ScoredChunk> scoredChunks = embeddingStore.findRelevantWithScores(q, topK * 3);
            if (scoredChunks != null) {
                for (InMemoryEmbeddingStore.ScoredChunk sc : scoredChunks) {
                    String text = sc.getChunk().segment.text();
                    if (!seenTexts.contains(text)) {
                        seenTexts.add(text);
                        mergedScoredChunks.add(sc);
                    }
                }
            }
        }

        List<EmbeddedChunk> mergedChunks = mergedScoredChunks.stream()
            .map(InMemoryEmbeddingStore.ScoredChunk::getChunk)
            .toList();

        List<RagChunkInfo> chunkInfos = mergedScoredChunks.stream()
            .map(sc -> new RagChunkInfo(sc.getChunk().segment.text(), sc.getScore()))
            .toList();
        tracer.logRetrieval(chunkInfos.size());
        tracer.logRetrievalDetails(chunkInfos);

        List<EmbeddedChunk> rerankedChunks = rerankAndFilter(originalQuestion, mergedChunks, topK);

        List<RerankEntry> beforeEntries = mergedScoredChunks.stream()
            .map(sc -> new RerankEntry(sc.getChunk().segment.text(), sc.getScore()))
            .toList();
        List<RerankEntry> afterEntries = rerankedChunks.stream()
            .map(c -> new RerankEntry(c.segment.text(), 0.0))
            .toList();
        tracer.logRerank(mergedChunks.size(), rerankedChunks.size(), 5.0);
        tracer.logRerankDetails(beforeEntries, afterEntries);

        StringBuilder context = new StringBuilder();
        int index = 1;
        for (EmbeddedChunk chunk : rerankedChunks) {
            String text = chunk.segment.text();
            if (text.length() < 50) continue;
            context.append("[").append(index).append("] ").append(text).append("\n\n");
            index++;
        }

        String prompt = buildPrompt(originalQuestion, context.toString());

        RagAssistant assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String answer = assistant.chat(prompt);
        tracer.logFinalContext(context.length(), prompt.length());
        tracer.logAnswer(answer);

        return new RagQueryResult(answer, mergedChunks.size(), rerankedChunks.size(), context.length(), prompt.length());
    }

    private List<EmbeddedChunk> rerankAndFilter(String question, List<EmbeddedChunk> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        String rerankTemplate = promptLoader.load(promptProperties.getRerank());
        StringBuilder chunksBuilder = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            chunksBuilder.append("[").append(i + 1).append("] ").append(candidates.get(i).segment.text()).append("\n\n");
        }
        String prompt = promptLoader.fill(rerankTemplate, Map.of(
            "question", question,
            "chunks", chunksBuilder.toString()
        ));

        Reranker reranker = AiServices.builder(Reranker.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String result = reranker.rerank(prompt);

        List<ScoredChunk> scored = new ArrayList<>();
        String[] lines = result.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("\\[\\d+\\]\\s*\\d+(\\.\\d+)?")) {
                String[] parts = line.split("\\]\\s*");
                int idx = Integer.parseInt(parts[0].substring(1)) - 1;
                double score = Double.parseDouble(parts[1]);
                if (idx >= 0 && idx < candidates.size() && score >= 0.5) {
                    scored.add(new ScoredChunk(candidates.get(idx), score));
                }
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<EmbeddedChunk> resultChunks = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            resultChunks.add(scored.get(i).chunk);
        }
        return resultChunks;
    }

    private record ScoredChunk(EmbeddedChunk chunk, double score) {}

    private List<String> generateMultipleQueries(String query) {
        String template = promptLoader.load(promptProperties.getMultiQuery());
        String prompt = promptLoader.fill(template, "query", query);

        QueryExpander expander = AiServices.builder(QueryExpander.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String result = expander.expand(prompt);
        List<String> queries = new ArrayList<>();
        for (String line : result.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && queries.size() < 3) {
                queries.add(trimmed);
            }
        }
        if (queries.isEmpty()) {
            queries.add(query);
        }
        return queries;
    }

    private String buildPrompt(String question, String context) {
        String template = promptLoader.load(promptProperties.getGrounding());
        return promptLoader.fill(template, Map.of(
            "question", question,
            "prompt", context
        ));
    }

    public void clear() {
        embeddingStore.clear();
    }

    public int getDocumentCount() {
        return embeddingStore.size();
    }

    private int calculateDynamicTopK(int totalChunks, int requestedTopK) {
        if (totalChunks <= 0) {
            return Math.max(2, requestedTopK);
        }
        if (totalChunks < 3) {
            return totalChunks;
        }
        if (totalChunks <= 10) {
            return Math.min(3 + (totalChunks - 3) / 2, 5);
        }
        return 5;
    }

    public interface RagAssistant {
        String chat(String message);
    }

    public interface QueryExpander {
        String expand(String prompt);
    }

    public interface Reranker {
        String rerank(String prompt);
    }

    public record CitationChunk(int index, String content, double score) {}

    public void streamQuery(String question, SseEmitter emitter) {
        try {
            int totalChunks = embeddingStore.size();

            if (totalChunks == 0) {
                streamDirectAnswer(question, emitter);
                return;
            }

            List<String> queries = generateMultipleQueries(question);
            int topK = calculateDynamicTopK(totalChunks, 5);

            Set<String> seenTexts = new HashSet<>();
            List<InMemoryEmbeddingStore.ScoredChunk> mergedScoredChunks = new ArrayList<>();
            for (String q : queries) {
                List<InMemoryEmbeddingStore.ScoredChunk> scoredChunks = embeddingStore.findRelevantWithScores(q, topK * 3);
                if (scoredChunks != null) {
                    for (InMemoryEmbeddingStore.ScoredChunk sc : scoredChunks) {
                        String text = sc.getChunk().segment.text();
                        if (!seenTexts.contains(text)) {
                            seenTexts.add(text);
                            mergedScoredChunks.add(sc);
                        }
                    }
                }
            }

            List<EmbeddedChunk> mergedChunks = mergedScoredChunks.stream()
                .map(InMemoryEmbeddingStore.ScoredChunk::getChunk)
                .toList();

            List<EmbeddedChunk> rerankedChunks = rerankAndFilter(question, mergedChunks, topK);

            StringBuilder context = new StringBuilder();
            List<CitationChunk> citationChunks = new ArrayList<>();
            int index = 1;
            for (EmbeddedChunk chunk : rerankedChunks) {
                String text = chunk.segment.text();
                if (text.length() < 50) continue;
                context.append("[").append(index).append("] ").append(text).append("\n\n");
                citationChunks.add(new CitationChunk(index, text, 0.0));
                index++;
            }

            String metadataJson = buildMetadataJson(citationChunks);
            emitter.send(SseEmitter.event().name("metadata").data(metadataJson));

            String prompt = buildPrompt(question, context.toString());
            RagAssistant assistant = AiServices.builder(RagAssistant.class)
                    .chatLanguageModel(modelFactory.getDefaultModel())
                    .build();

            String fullResponse = assistant.chat(prompt);

            for (char c : fullResponse.toCharArray()) {
                emitter.send(SseEmitter.event().name("message").data(String.valueOf(c)));
                Thread.sleep(20);
            }

            emitter.send(SseEmitter.event().name("message").data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void streamDirectAnswer(String question, SseEmitter emitter) throws Exception {
        emitter.send(SseEmitter.event().name("metadata").data("[]"));

        RagAssistant assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String fullResponse = assistant.chat(question);

        for (char c : fullResponse.toCharArray()) {
            emitter.send(SseEmitter.event().name("message").data(String.valueOf(c)));
            Thread.sleep(20);
        }

        emitter.send(SseEmitter.event().name("message").data("[DONE]"));
        emitter.complete();
    }

    private String buildMetadataJson(List<CitationChunk> chunks) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < chunks.size(); i++) {
            CitationChunk chunk = chunks.get(i);
            if (i > 0) json.append(",");
            json.append("{\"index\":").append(chunk.index())
                .append(",\"content\":\"").append(escapeJson(chunk.content())).append("\"")
                .append(",\"score\":").append(chunk.score()).append("}");
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}