package com.mindhr.service;

import com.mindhr.dto.ContractDocumentRequest;
import com.mindhr.dto.ContractDocumentResponse;
import com.mindhr.entity.Contract;
import com.mindhr.entity.ContractDocument;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.ContractDocumentMapper;
import com.mindhr.repository.ContractDocumentRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** No update: replacing a document is a delete + re-upload, not an edit. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractDocumentService {

    private final ContractDocumentRepository contractDocumentRepository;
    private final ContractService contractService;
    private final UserRepository userRepository;

    public List<ContractDocumentResponse> list(Long contractId) {
        contractService.findAccessibleEntity(contractId);
        return contractDocumentRepository.findByContractIdOrderByUploadedAtDesc(contractId).stream()
                .map(ContractDocumentMapper::toResponse)
                .toList();
    }

    @Transactional
    public ContractDocumentResponse create(Long contractId, ContractDocumentRequest request) {
        Contract contract = contractService.findAccessibleEntity(contractId);

        ContractDocument document = new ContractDocument();
        document.setContract(contract);
        document.setFileName(request.fileName());
        document.setFileUrl(request.fileUrl());
        document.setContentType(request.contentType());
        document.setFileSize(request.fileSize());
        document.setUploadedAt(LocalDateTime.now());
        userRepository.findById(TenantContext.currentPrincipal().userId()).ifPresent(document::setUploadedBy);

        return ContractDocumentMapper.toResponse(contractDocumentRepository.save(document));
    }

    @Transactional
    public void delete(Long id) {
        ContractDocument document = contractDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract document not found: " + id));
        if (!TenantContext.canAccess(document.getContract().getOrganization().getId())) {
            throw new ResourceNotFoundException("Contract document not found: " + id);
        }
        contractDocumentRepository.delete(document);
    }
}
