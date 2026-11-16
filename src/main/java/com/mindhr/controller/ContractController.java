package com.mindhr.controller;

import com.mindhr.dto.ContractDocumentRequest;
import com.mindhr.dto.ContractDocumentResponse;
import com.mindhr.dto.ContractHistoryResponse;
import com.mindhr.dto.ContractRequest;
import com.mindhr.dto.ContractResponse;
import com.mindhr.dto.ContractStatusChangeRequest;
import com.mindhr.service.ContractDocumentService;
import com.mindhr.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractDocumentService contractDocumentService;

    @GetMapping
    public ResponseEntity<List<ContractResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(contractService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ContractHistoryResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.history(id));
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ResponseEntity.status(201).body(contractService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractResponse> update(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ContractResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ContractStatusChangeRequest request) {
        return ResponseEntity.ok(contractService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<ContractDocumentResponse>> listDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(contractDocumentService.list(id));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ContractDocumentResponse> addDocument(
            @PathVariable Long id, @Valid @RequestBody ContractDocumentRequest request) {
        return ResponseEntity.status(201).body(contractDocumentService.create(id, request));
    }
}
