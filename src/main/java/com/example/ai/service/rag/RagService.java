package com.example.ai.service.rag;

import com.example.ai.infrastructure.embedding.EmbeddingStore;
import com.example.ai.infrastructure.embedding.EmbeddedChunk;
import com.example.ai.model.dto.RagRequest;
import com.example.ai.rag.*;
import com.example.ai.service.llm.ModelFactory;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        List<EmbeddedChunk> relevantChunks =
                embeddingStore.findRelevant(request.getQuestion(), request.getTopK());

        StringBuilder context = new StringBuilder();
        if (relevantChunks != null) {
            for (int i = 0; i < relevantChunks.size(); i++) {
                context.append("[").append(i + 1).append("] ")
                       .append(relevantChunks.get(i).segment.text())
                       .append("\n\n");
            }
        }

        String prompt = buildPrompt(request.getQuestion(), context.toString());

        RagAssistant assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(modelFactory.getDefaultModel())
                .build();

        return assistant.chat(prompt);
    }

    private String buildPrompt(String question, String context) {
        return """
                你是一个知识库问答助手，任务是基于提供的参考资料回答用户问题。

                ## 回答要求

                1. **必须基于参考资料回答**，不得编造不在资料中的信息
                2. **如果资料中没有相关信息，直接回答"资料中没有提供相关信息"**，不要推测
                3. **用中文回答**
                4. **分点输出**，使用编号列表，每个要点单独一行

                ## 参考资料

                %s

                ## 用户问题

                %s

                ## 回答格式

                在回答每个要点时，在句末用 [编号] 标注信息来源。例如：[1][3]

                如果某个要点来自多个来源，请标注所有相关编号：[1][2]
                """.formatted(context, question);
    }

    public void clear() {
        embeddingStore.clear();
    }

    public int getDocumentCount() {
        return embeddingStore.size();
    }

    public interface RagAssistant {
        String chat(String message);
    }
}