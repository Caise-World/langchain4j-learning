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
            for (EmbeddedChunk chunk : relevantChunks) {
                context.append(chunk.segment.text()).append("\n\n");
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
                你是一个知识库问答助手。请根据以下参考资料回答用户的问题。

                参考资料：
                %s

                用户问题：%s

                请基于参考资料回答，如果参考资料中没有相关信息，请如实说明。
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