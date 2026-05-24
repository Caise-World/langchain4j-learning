package com.example.ai.controller;

import com.example.ai.common.Result;
import com.example.ai.model.dto.RagRequest;
import com.example.ai.model.dto.UploadResponse;
import com.example.ai.model.entity.UploadedDocument;
import com.example.ai.service.rag.DocumentService;
import com.example.ai.service.rag.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final DocumentService documentService;

    @PostMapping("/upload")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
        // 先保存文件到磁盘
        UploadedDocument savedDoc = documentService.saveDocument(file);

        // 用保存后的文件路径加载文档
        int chunkCount = ragService.ingestDocument(savedDoc.getFilePath());

        UploadResponse response = UploadResponse.builder()
                .filename(file.getOriginalFilename())
                .chunkCount(chunkCount)
                .message("Document indexed successfully")
                .build();

        return Result.success(response);
    }

    @PostMapping("/query")
    public Result<Map<String, String>> query(@Valid @RequestBody RagRequest request) {
        String answer = ragService.query(request);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        return Result.success(result);
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        ragService.clear();
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<Map<String, Integer>> stats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("documentCount", ragService.getDocumentCount());
        return Result.success(stats);
    }
}