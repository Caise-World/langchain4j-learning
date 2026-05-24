package com.example.ai.service.rag;

import com.example.ai.infrastructure.embedding.EmbeddingStore;
import com.example.ai.infrastructure.embedding.EmbeddedChunk;
import com.example.ai.model.dto.RagRequest;
import com.example.ai.rag.*;
import com.example.ai.service.llm.ModelFactory;
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
        List<String> queries = generateMultipleQueries(originalQuestion);

        int totalChunks = embeddingStore.size();
        int topK = calculateDynamicTopK(totalChunks, request.getTopK());

        // 多 query 分别检索，合并结果
        Set<String> seenTexts = new HashSet<>();
        List<EmbeddedChunk> mergedChunks = new ArrayList<>();
        for (String q : queries) {
            List<EmbeddedChunk> chunks = embeddingStore.findRelevant(q, topK);
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

        StringBuilder context = new StringBuilder();
        int index = 1;
        for (EmbeddedChunk chunk : mergedChunks) {
            String text = chunk.segment.text();
            if (text.length() < 50) continue;
            context.append("[").append(index).append("] ").append(text).append("\n\n");
            index++;
        }

        String prompt = buildPrompt(originalQuestion, context.toString());

        RagAssistant assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        return assistant.chat(prompt);
    }

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
}