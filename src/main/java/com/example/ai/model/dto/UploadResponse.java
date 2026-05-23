package com.example.ai.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {
    private Long documentId;
    private String filename;
    private Integer chunkCount;
    private String message;
}