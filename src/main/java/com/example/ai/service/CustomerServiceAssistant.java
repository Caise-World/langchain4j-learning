package com.example.ai.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CustomerServiceAssistant {

    @UserMessage("""
            你是一个热情的在线客服助手。

            客户信息：
            - 客户姓名：{{customerName}}
            - 咨询产品：{{productName}}

            客户问题：{{question}}

            请用友好、专业的语气回答客户的问题。
            """)
    String answer(
            @V("customerName") String customerName,
            @V("productName") String productName,
            @V("question") String question
    );
}