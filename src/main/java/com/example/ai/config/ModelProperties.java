package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai-models")
public class ModelProperties {
    private ModelConfig minimax = new ModelConfig();
    private ModelConfig glm4 = new ModelConfig();
    private ModelConfig openai = new ModelConfig();

    @Data
    public static class ModelConfig {
        private String apiKey;
        private String modelName;
        private String baseUrl;
        private double temperature = 0.7;
        private int maxTokens = 1024;
    }
}