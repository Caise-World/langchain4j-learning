package com.example.ai.controller;

import com.example.ai.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class StreamingChatController {

    private final LlmService llmService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message,
                                 @RequestParam(required = false) String sessionId,
                                 @RequestParam(required = false) String modelType) {
        SseEmitter emitter = new SseEmitter();

        llmService.streamChat(message, sessionId, modelType, emitter);

        return emitter;
    }
}