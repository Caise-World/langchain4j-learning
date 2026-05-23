package com.example.ai.service.llm;

import com.example.ai.model.enums.ModelType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final Map<ModelType, ChatLanguageModel> chatModels;

    public ChatLanguageModel getModel(ModelType type) {
        ChatLanguageModel model = chatModels.get(type);
        if (model == null) {
            return getDefaultModel();
        }
        return model;
    }

    public ChatLanguageModel getDefaultModel() {
        ChatLanguageModel model = chatModels.get(ModelType.MINIMAX);
        if (model == null) {
            model = chatModels.values().stream().findFirst().orElse(null);
        }
        if (model == null) {
            throw new IllegalStateException("No AI model configured");
        }
        return model;
    }
}