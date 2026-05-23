package com.example.ai.model.enums;

public enum ModelType {
    MINIMAX("minimax-m2.7", "https://api.minimax.chat/v1"),
    GLM4("glm-4", "https://open.bigmodel.cn/api/paas/v4"),
    OPENAI("gpt-4o", "https://api.openai.com/v1");

    private final String modelName;
    private final String baseUrl;

    ModelType(String modelName, String baseUrl) {
        this.modelName = modelName;
        this.baseUrl = baseUrl;
    }

    public String getModelName() { return modelName; }
    public String getBaseUrl() { return baseUrl; }
}