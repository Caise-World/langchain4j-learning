package com.example.ai.config;

import com.example.ai.rag.TextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TextSplitter textSplitter() {
        return new TextSplitter(500, 50);
    }
}