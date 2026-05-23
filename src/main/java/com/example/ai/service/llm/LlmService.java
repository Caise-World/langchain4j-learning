package com.example.ai.service.llm;

import com.example.ai.model.dto.ChatRequest;
import com.example.ai.model.dto.ChatResponse;
import com.example.ai.model.enums.ModelType;
import com.example.ai.model.enums.MessageRole;
import com.example.ai.service.tool.ToolService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final ModelFactory modelFactory;
    private final ChatMemoryService memoryService;
    private final ToolService toolService;

    public ChatResponse chat(ChatRequest request) {
        ModelType modelType = parseModelType(request.getModelType());
        ChatLanguageModel model = modelFactory.getModel(modelType);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .build();

        String response;
        if (Boolean.TRUE.equals(request.getEnableMemory()) && request.getSessionId() != null) {
            response = chatWithMemory(request, model, assistant);
        } else if (Boolean.TRUE.equals(request.getEnableTools())) {
            response = chatWithTools(request, model, assistant);
        } else {
            response = assistant.chat(request.getMessage());
        }

        return ChatResponse.builder()
                .message(response)
                .sessionId(request.getSessionId())
                .modelType(modelType.name())
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();
    }

    private String chatWithMemory(ChatRequest request, ChatLanguageModel model, Assistant assistant) {
        String sessionId = request.getSessionId();

        if (sessionId == null) {
            sessionId = memoryService.createSession(request.getModelType());
            request.setSessionId(sessionId);
        }

        var history = memoryService.getHistory(sessionId);

        memoryService.saveMessage(sessionId, MessageRole.USER.name(), request.getMessage());

        StringBuilder promptBuilder = new StringBuilder();
        for (var msg : history) {
            promptBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        promptBuilder.append("user: ").append(request.getMessage());

        String response = assistant.chat(promptBuilder.toString());

        memoryService.saveMessage(sessionId, MessageRole.ASSISTANT.name(), response);

        return response;
    }

    private String chatWithTools(ChatRequest request, ChatLanguageModel model, Assistant assistant) {
        return assistant.chat(request.getMessage());
    }

    private ModelType parseModelType(String modelType) {
        if (modelType == null || modelType.isEmpty()) {
            return ModelType.MINIMAX;
        }
        try {
            return ModelType.valueOf(modelType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ModelType.MINIMAX;
        }
    }

    public interface Assistant {
        String chat(String message);
    }
}