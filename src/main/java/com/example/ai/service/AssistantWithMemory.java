package com.example.ai.service;

import dev.langchain4j.service.UserMessage;

/**
 * 对话助手接口 - 带 Memory
 */
public interface AssistantWithMemory {

    @UserMessage("{{content}}")
    String chat(String content);
}