package com.example.ai.service;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmService {

    private final Assistant assistant;
    private final CustomerServiceAssistant customerAssistant;
    private final ChatLanguageModel chatModel;
    private final Map<String, AssistantWithMemory> memoryMap = new ConcurrentHashMap<>();

    public LlmService(Assistant assistant, CustomerServiceAssistant customerAssistant, ChatLanguageModel chatModel) {
        this.assistant = assistant;
        this.customerAssistant = customerAssistant;
        this.chatModel = chatModel;
        memoryMap.put("default", createAssistantWithMemory());
    }

    private AssistantWithMemory createAssistantWithMemory() {
        return AiServices.builder(AssistantWithMemory.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.builder().id("default").maxMessages(10).build())
                .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }

    /**
     * 带工具调用的聊天 - AI会自动选择工具执行
     */
    public String chatWithTools(String message) {
        return assistant.chat(message);
    }

    /**
     * 客服场景 - 使用 Prompt 模板
     */
    public String customerServiceChat(String customerName, String productName, String question) {
        return customerAssistant.answer(customerName, productName, question);
    }

    /**
     * 带记忆的聊天 - 根据 sessionId 区分不同会话
     */
    public String chatWithMemory(String sessionId, String message) {
        AssistantWithMemory assistant = memoryMap.computeIfAbsent(sessionId, id -> createNewAssistantWithMemory(id));
        return assistant.chat(message);
    }

    private AssistantWithMemory createNewAssistantWithMemory(String sessionId) {
        return AiServices.builder(AssistantWithMemory.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.builder().id(sessionId).maxMessages(10).build())
                .build();
    }
}