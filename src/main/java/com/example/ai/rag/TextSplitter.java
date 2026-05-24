package com.example.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSplitter {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
        "[。！？.!?]+"
    );

    private final int minChunkChars;
    private final int maxChunkChars;
    private final int overlapSentences;

    public TextSplitter(int minChunkChars, int maxChunkChars, int overlapSentences) {
        this.minChunkChars = minChunkChars;
        this.maxChunkChars = maxChunkChars;
        this.overlapSentences = overlapSentences;
    }

    public TextSplitter() {
        this(200, 400, 1);
    }

    public List<TextChunk> split(String text, String documentId) {
        List<TextChunk> chunks = new ArrayList<>();
        List<String> sentences = splitIntoSentences(text);
        List<String> currentSentences = new ArrayList<>();
        int currentLength = 0;
        int chunkIndex = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceLen = sentence.length();

            if (currentLength + sentenceLen > maxChunkChars && currentLength >= minChunkChars) {
                chunks.add(new TextChunk(
                    documentId + "_chunk_" + chunkIndex,
                    String.join("", currentSentences),
                    documentId
                ));
                chunkIndex++;

                int overlapCount = Math.min(overlapSentences, currentSentences.size());
                List<String> overlap = new ArrayList<>();
                for (int j = currentSentences.size() - overlapCount; j < currentSentences.size(); j++) {
                    overlap.add(currentSentences.get(j));
                }

                currentSentences.clear();
                currentSentences.addAll(overlap);
                currentLength = overlap.stream().mapToInt(String::length).sum();
            }

            currentSentences.add(sentence);
            currentLength += sentenceLen;
        }

        if (!currentSentences.isEmpty()) {
            chunks.add(new TextChunk(
                documentId + "_chunk_" + chunkIndex,
                String.join("", currentSentences),
                documentId
            ));
        }

        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = SENTENCE_PATTERN.split(text);
        Matcher matcher = SENTENCE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentenceContent = text.substring(lastEnd, matcher.start()).trim();
            String punctuation = matcher.group();
            if (!sentenceContent.isEmpty()) {
                sentences.add(sentenceContent + punctuation);
            }
            lastEnd = matcher.end();
        }

        String remaining = text.substring(lastEnd).trim();
        if (!remaining.isEmpty()) {
            sentences.add(remaining);
        }

        return sentences;
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