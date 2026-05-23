package com.example.ai.controller;

import com.example.ai.service.LlmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final LlmService llmService;

    public AiController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 普通聊天（无记忆）
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return llmService.chat(message);
    }

    /**
     * 带记忆的聊天 - 连续对话
     * sessionId: 会话ID，同一会话ID共享对话历史
     * message: 消息内容
     */
    @GetMapping("/memory/chat")
    public String chatWithMemory(
            @RequestParam String sessionId,
            @RequestParam String message) {
        return llmService.chatWithMemory(sessionId, message);
    }

    /**
     * 客服场景 - 带 Prompt 模板
     */
    @GetMapping("/customer")
    public String customerChat(
            @RequestParam String customerName,
            @RequestParam String productName,
            @RequestParam String question) {
        return llmService.customerServiceChat(customerName, productName, question);
    }

    /**
     * 带工具调用的聊天 - AI会自动选择工具执行
     */
    @GetMapping("/tools/chat")
    public String chatWithTools(@RequestParam String message) {
        return llmService.chatWithTools(message);
    }
}