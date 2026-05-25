package com.example.ai.service.rag;

import com.example.ai.infrastructure.embedding.EmbeddingStore;
import com.example.ai.infrastructure.embedding.EmbeddedChunk;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ModelFactory modelFactory;
    private final TextSplitter textSplitter;
    private final EmbeddingStore embeddingStore;
    private final RagTracer tracer;

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

        // 多 query 分别检索，合并候选（扩大初筛范围用于 rerank）
        Set<String> seenTexts = new HashSet<>();
        List<EmbeddedChunk> mergedChunks = new ArrayList<>();
        for (String q : queries) {
            List<EmbeddedChunk> chunks = embeddingStore.findRelevant(q, topK * 3);  // 初筛扩大
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

        // rerank 排序
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

        return new RagQueryResult(answer, mergedChunks.size(), rerankedChunks.size(), context.length(), prompt.length());
    }

    private List<EmbeddedChunk> rerankAndFilter(String question, List<EmbeddedChunk> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        // 构建 rerank prompt
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个相关性评分助手，请根据用户问题给每个候选文本打分（0-10）：\n\n");
        sb.append("评分标准：\n");
        sb.append("- 是否直接回答问题\n");
        sb.append("- 是否包含关键信息\n");
        sb.append("- 是否语义相关\n\n");
        sb.append("输出格式：\n");
        sb.append("[编号] 分数\n\n");
        sb.append("用户问题：").append(question).append("\n\n");
        sb.append("候选文本：\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(candidates.get(i).segment.text()).append("\n\n");
        }

        Reranker reranker = AiServices.builder(Reranker.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String result = reranker.rerank(sb.toString());

        // 解析分数并排序
        List<ScoredChunk> scored = new ArrayList<>();
        String[] lines = result.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("\\[\\d+\\]\\s*\\d+")) {
                String[] parts = line.split("\\]\\s*");
                int idx = Integer.parseInt(parts[0].substring(1)) - 1;
                double score = Double.parseDouble(parts[1]);
                if (idx >= 0 && idx < candidates.size() && score >= 5) {
                    scored.add(new ScoredChunk(candidates.get(idx), score));
                }
            }
        }

        // 按分数降序，取 topK
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<EmbeddedChunk> resultChunks = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            resultChunks.add(scored.get(i).chunk);
        }
        return resultChunks;
    }

    private record ScoredChunk(EmbeddedChunk chunk, double score) {}

    private List<String> generateMultipleQueries(String query) {
        String multiQueryPrompt = """
                你是检索优化助手，请基于用户问题生成3个不同的检索版本：
                要求：
                - 保留原意
                - 从不同角度表达
                - 包含关键词变体
                - 输出3条，用换行分隔，不要解释

                用户问题：%s
                """.formatted(query);

        QueryExpander expander = AiServices.builder(QueryExpander.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        String result = expander.expand(multiQueryPrompt);
        List<String> queries = new ArrayList<>();
        for (String line : result.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && queries.size() < 3) {
                queries.add(trimmed);
            }
        }
        // 至少保含原始查询
        if (queries.isEmpty()) {
            queries.add(query);
        }
        return queries;
    }

    private String buildPrompt(String question, String context) {
        return """
                你是一个严格基于资料的知识库问答助手。

                ## 核心原则

                1. **只使用提供的参考资料回答**，不得编造任何资料中不存在的信息
                2. **禁止使用常识或推理补全资料中未提供的信息**
                3. **如果资料不足以完整回答，必须明确回答"根据现有资料无法回答该问题"**
                4. **用中文回答**

                ## 结构化输出要求

                回答必须包含以下三部分，缺一不可：

                ### 一、答案
                基于资料给出答案，每条结论必须标注[编号]

                ### 二、引用来源
                所有引用的编号必须对应当前context中的chunk顺序

                ### 三、无匹配说明（若无相关资料）
                如果资料中没有相关信息，回答："根据现有资料无法回答该问题"

                ## 严格引用规则

                - 每个结论必须有对应chunk编号
                - 不允许无引用的结论
                - 若无法引用 → 删除该结论
                - 禁止跨chunk混用引用

                ## 参考资料

                %s

                ## 用户问题

                %s

                ## 回答格式

                答案：
                1. [结论内容] [编号]
                2. [结论内容] [编号]

                来源：[编号列表]

                （如果无相关资料，直接输出：根據現有資料無法回答該問題）
                """.formatted(context, question);
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
}