package com.example.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
    private String modelType;
    private String sessionId;
    private Boolean enableMemory = false;
    private Boolean enableTools = false;
}