package com.example.ai.controller;

import com.example.ai.common.Result;
import com.example.ai.model.dto.ChatRequest;
import com.example.ai.model.dto.ChatResponse;
import com.example.ai.model.entity.ChatMessage;
import com.example.ai.service.llm.ChatMemoryService;
import com.example.ai.service.llm.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LlmService llmService;
    private final ChatMemoryService memoryService;

    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(llmService.chat(request));
    }

    @GetMapping("/history/{sessionId}")
    public Result<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        return Result.success(memoryService.getHistory(sessionId));
    }
}