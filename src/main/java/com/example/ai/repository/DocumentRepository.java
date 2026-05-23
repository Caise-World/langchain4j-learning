package com.example.ai.repository;

import com.example.ai.model.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<UploadedDocument, Long> {
}