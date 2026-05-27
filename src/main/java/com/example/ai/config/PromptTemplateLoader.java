package com.example.ai.config;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptTemplateLoader {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    public String load(String classpathPath) {
        String path = classpathPath;
        if (path.startsWith("classpath:")) {
            path = path.substring("classpath:".length());
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Prompt file not found: " + classpathPath + " (tried path: " + path + ")");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + classpathPath, e);
        }
    }

    public String fill(String template, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public String fill(String template, String key, String value) {
        return fill(template, Map.of(key, value));
    }
}