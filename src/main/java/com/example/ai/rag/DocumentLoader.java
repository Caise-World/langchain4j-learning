package com.example.ai.rag;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DocumentLoader {

    public Document load(String filePath) throws IOException {
        Path path = Path.of(filePath);
        String content = Files.readString(path);
        String fileName = path.getFileName().toString();
        String id = UUID.randomUUID().toString();
        return new Document(id, content, fileName);
    }

    public List<Document> loadMultiple(List<String> filePaths) {
        return filePaths.stream()
                .map(path -> {
                    try {
                        return load(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load: " + path, e);
                    }
                })
                .collect(Collectors.toList());
    }
}