package com.example.ai.config;

import com.example.ai.rag.InMemoryEmbeddingStore;
import com.example.ai.service.Assistant;
import com.example.ai.service.CustomerServiceAssistant;
import com.example.ai.tool.CalculatorTool;
import com.example.ai.tool.MockWeatherTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("minimax-m2.7")
                .baseUrl("https://api.minimax.chat/v1")
                .temperature(0.7)
                .maxTokens(1024)
                .build();
    }

    @Bean
    public InMemoryEmbeddingStore embeddingStore() {
        return new InMemoryEmbeddingStore();
    }

    @Bean
    public Assistant assistant(ChatLanguageModel chatModel, CalculatorTool calculatorTool, MockWeatherTool weatherTool) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .tools(calculatorTool, weatherTool)
                .build();
    }

    @Bean
    public CustomerServiceAssistant customerServiceAssistant(ChatLanguageModel chatModel) {
        return AiServices.builder(CustomerServiceAssistant.class)
                .chatLanguageModel(chatModel)
                .build();
    }
}