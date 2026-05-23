package com.example.ai.exception;

public enum ErrorCode {
    MODEL_NOT_FOUND("AI_001", "Model not found"),
    INVALID_REQUEST("AI_002", "Invalid request"),
    SESSION_NOT_FOUND("AI_003", "Session not found"),
    DOCUMENT_LOAD_FAILED("AI_004", "Failed to load document"),
    UPLOAD_FAILED("AI_005", "File upload failed"),
    INTERNAL_ERROR("AI_500", "Internal server error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}