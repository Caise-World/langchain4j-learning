package com.example.ai.rag;

import java.util.List;

public class Document {
    private final String id;
    private final String content;
    private final String source;

    public Document(String id, String content, String source) {
        this.id = id;
        this.content = content;
        this.source = source;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public String getSource() { return source; }

    public List<String> getContentAsList() {
        return List.of(content.split("\n"));
    }
}