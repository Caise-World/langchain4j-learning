package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "prompts")
public class PromptProperties {
    private String grounding;
    private String rerank;
    private String multiQuery;
}