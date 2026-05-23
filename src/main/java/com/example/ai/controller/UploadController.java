package com.example.ai.controller;

import com.example.ai.common.Result;
import com.example.ai.model.dto.UploadResponse;
import com.example.ai.model.entity.UploadedDocument;
import com.example.ai.service.rag.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final DocumentService documentService;

    @PostMapping
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
        UploadedDocument doc = documentService.saveDocument(file);

        UploadResponse response = UploadResponse.builder()
                .documentId(doc.getId())
                .filename(doc.getFilename())
                .chunkCount(0)
                .message("File uploaded successfully")
                .build();

        return Result.success(response);
    }
}