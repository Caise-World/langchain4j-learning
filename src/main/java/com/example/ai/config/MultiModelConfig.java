package com.example.ai.config;

import com.example.ai.infrastructure.embedding.EmbeddingStore;
import com.example.ai.infrastructure.embedding.InMemoryEmbeddingStore;
import com.example.ai.infrastructure.embedding.PgVectorEmbeddingStore;
import com.example.ai.model.enums.ModelType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiModelConfig {

    private final ModelProperties modelProperties;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public EmbeddingStore embeddingStore(EntityManager em) {
        if ("pgvector".equals(activeProfile)) {
            log.info("Using PgVectorEmbeddingStore");
            return new PgVectorEmbeddingStore(em);
        }
        log.info("Using InMemoryEmbeddingStore");
        return new InMemoryEmbeddingStore();
    }

    @Bean
    public Map<ModelType, ChatLanguageModel> chatModels() {
        Map<ModelType, ChatLanguageModel> models = new EnumMap<>(ModelType.class);

        // 只配置 MiniMax
        String minimaxKey = modelProperties.getMinimax().getApiKey();
        if (minimaxKey != null && !minimaxKey.isEmpty() && !minimaxKey.contains("MINIMAX_API_KEY")) {
            log.info("Configuring MiniMax model");
            models.put(ModelType.MINIMAX, OpenAiChatModel.builder()
                    .apiKey(minimaxKey)
                    .modelName("minimax-m2.7")
                    .baseUrl("https://api.minimax.chat/v1")
                    .temperature(0.7)
                    .maxTokens(1024)
                    .build());
        }

        return models;
    }
}