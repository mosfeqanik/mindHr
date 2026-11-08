package com.mindhr.controller;

import com.mindhr.service.ContractDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Deletion by document id lives here; listing/uploading is nested under {@code /api/contracts/{id}/documents}. */
@RestController
@RequestMapping("/api/contract-documents")
@RequiredArgsConstructor
public class ContractDocumentController {

    private final ContractDocumentService contractDocumentService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
