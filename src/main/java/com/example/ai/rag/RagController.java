package com.example.ai.rag;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Path tempFile = Path.of("/tmp/" + UUID.randomUUID() + "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            ragService.ingestDocument(tempFile.toString());
            return "Document indexed successfully! Chunks: " + ragService.getDocumentCount();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/query")
    public String query(@RequestParam String question) {
        return ragService.retrieveAndAnswer(question);
    }

    @DeleteMapping("/clear")
    public String clear() {
        ragService.clear();
        return "Knowledge base cleared!";
    }
}