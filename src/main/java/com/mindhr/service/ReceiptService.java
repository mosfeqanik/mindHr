package com.mindhr.service;

import com.mindhr.dto.ReceiptResponse;
import com.mindhr.entity.Receipt;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.ReceiptMapper;
import com.mindhr.repository.ReceiptRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only: receipts are only ever created via {@code PaymentService#issueReceipt}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    public List<ReceiptResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return receiptRepository.findByOrganizationId(organizationId).stream()
                .map(ReceiptMapper::toResponse)
                .toList();
    }

    public ReceiptResponse getById(Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + id));
        if (!TenantContext.canAccess(receipt.getOrganization().getId())) {
            throw new ResourceNotFoundException("Receipt not found: " + id);
        }
        return ReceiptMapper.toResponse(receipt);
    }
}
