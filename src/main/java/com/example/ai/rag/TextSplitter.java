package com.example.ai.rag;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    private final int chunkSize;
    private final int overlap;

    public TextSplitter(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public TextSplitter() {
        this(500, 50);
    }

    public List<TextChunk> split(String text, String documentId) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String line : lines) {
            if (currentChunk.length() + line.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(new TextChunk(
                    documentId + "_chunk_" + chunkIndex,
                    currentChunk.toString().trim(),
                    documentId
                ));
                chunkIndex++;
                String overlapText = currentChunk.toString();
                currentChunk = new StringBuilder();
                if (overlap > 0 && overlapText.length() > overlap) {
                    currentChunk.append(overlapText.substring(overlapText.length() - overlap));
                }
            }
            currentChunk.append(line).append("\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(new TextChunk(
                documentId + "_chunk_" + chunkIndex,
                currentChunk.toString().trim(),
                documentId
            ));
        }

        return chunks;
    }

    public static class TextChunk {
        private final String id;
        private final String content;
        private final String parentId;

        public TextChunk(String id, String content, String parentId) {
            this.id = id;
            this.content = content;
            this.parentId = parentId;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public String getParentId() { return parentId; }
    }
}