package com.example.ai.config;

import com.example.ai.model.enums.ModelType;
import com.example.ai.rag.InMemoryEmbeddingStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiModelConfig {

    private final ModelProperties modelProperties;

    @Bean
    public InMemoryEmbeddingStore embeddingStore() {
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