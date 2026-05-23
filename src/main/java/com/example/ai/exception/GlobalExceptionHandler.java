package com.example.ai.exception;

import com.example.ai.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("Business error: {}", e.getMessage());
        return Result.error(500, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage());
        return Result.error(400, "VALIDATION_ERROR: " + e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.error("File too large: {}", e.getMessage());
        return Result.error(413, "File size exceeds maximum allowed size");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return Result.error(500, "An unexpected error occurred");
    }
}