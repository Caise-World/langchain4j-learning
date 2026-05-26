package com.example.ai.service.rag;

import com.example.ai.model.entity.UploadedDocument;
import com.example.ai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RagService ragService;

    public UploadedDocument saveDocument(MultipartFile file) throws Exception {
        Path tempFile = Path.of("/tmp/" + UUID.randomUUID() + "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        // Ingest into embedding store
        ragService.ingestDocument(tempFile.toString());

        UploadedDocument doc = UploadedDocument.builder()
                .filename(file.getOriginalFilename())
                .filePath(tempFile.toString())
                .fileSize(file.getSize())
                .build();

        return documentRepository.save(doc);
    }
}