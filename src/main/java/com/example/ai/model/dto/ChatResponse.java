package com.example.ai.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String message;
    private String sessionId;
    private String modelType;
    private String timestamp;
}