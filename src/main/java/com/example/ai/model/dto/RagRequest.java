package com.example.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagRequest {
    @NotBlank(message = "Question cannot be empty")
    private String question;
    private String modelType;
    private Integer topK = 5;
}