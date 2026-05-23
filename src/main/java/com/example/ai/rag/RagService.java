package com.example.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final ChatLanguageModel chatModel;
    private final DocumentLoader documentLoader;
    private final TextSplitter textSplitter;
    private final InMemoryEmbeddingStore embeddingStore;
    private final RagAssistant ragAssistant;

    public RagService(
            ChatLanguageModel chatModel,
            DocumentLoader documentLoader,
            InMemoryEmbeddingStore embeddingStore) {
        this.chatModel = chatModel;
        this.documentLoader = documentLoader;
        this.textSplitter = new TextSplitter(500, 50);
        this.embeddingStore = embeddingStore;
        this.ragAssistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(chatModel)
                .build();
    }

    public void ingestDocument(String filePath) throws Exception {
        Document doc = documentLoader.load(filePath);
        List<TextSplitter.TextChunk> chunks = textSplitter.split(doc.getContent(), doc.getId());

        for (TextSplitter.TextChunk chunk : chunks) {
            TextSegment segment = TextSegment.from(chunk.getContent());
            float[] vector = embeddingStore.createSimpleEmbedding(chunk.getContent());
            Embedding embedding = new Embedding(vector);
            embeddingStore.add(embedding, segment);
        }
    }

    public String retrieveAndAnswer(String question) {
        float[] questionVector = embeddingStore.createSimpleEmbedding(question);
        Embedding questionEmbedding = new Embedding(questionVector);

        List<InMemoryEmbeddingStore.EmbeddedChunk> relevantChunks =
                embeddingStore.findRelevantChunks(questionEmbedding, 5);

        StringBuilder context = new StringBuilder();
        for (InMemoryEmbeddingStore.EmbeddedChunk chunk : relevantChunks) {
            context.append(chunk.segment.text()).append("\n\n");
        }

        return ragAssistant.chat(question, context.toString());
    }

    public void clear() {
        embeddingStore.clear();
    }

    public int getDocumentCount() {
        return embeddingStore.size();
    }

    public interface RagAssistant {
        String chat(@UserMessage String question, @V("") String context);
    }
}