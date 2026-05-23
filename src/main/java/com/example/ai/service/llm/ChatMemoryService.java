package com.example.ai.service.llm;

import com.example.ai.model.entity.ChatMessage;
import com.example.ai.model.entity.ChatSession;
import com.example.ai.model.enums.MessageRole;
import com.example.ai.repository.ChatMessageRepository;
import com.example.ai.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public String createSession(String modelType) {
        ChatSession session = ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .modelType(modelType)
                .createdAt(LocalDateTime.now())
                .build();
        sessionRepository.save(session);
        return session.getSessionId();
    }

    public void saveMessage(String sessionId, String role, String content) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    ChatSession newSession = ChatSession.builder()
                            .sessionId(sessionId)
                            .modelType("MINIMAX")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return sessionRepository.save(newSession);
                });

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(message);
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .map(session -> messageRepository.findBySession_IdOrderByCreatedAtAsc(session.getId()))
                .orElse(List.of());
    }
}