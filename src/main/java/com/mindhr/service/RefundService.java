package com.mindhr.service;

import com.mindhr.dto.RefundRequest;
import com.mindhr.dto.RefundResponse;
import com.mindhr.dto.RefundStatusChangeRequest;
import com.mindhr.entity.Payment;
import com.mindhr.entity.Refund;
import com.mindhr.entity.enums.InvoiceStatus;
import com.mindhr.entity.enums.PaymentStatus;
import com.mindhr.entity.enums.RefundStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.RefundMapper;
import com.mindhr.repository.PaymentRepository;
import com.mindhr.repository.RefundRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An org user may request a refund for their own payment (status starts PENDING); only Finance
 * (platform staff) can move it to PROCESSED/REJECTED. Processing also marks the payment and its
 * invoice REFUNDED. No delete: a refund is a financial audit record.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public List<RefundResponse> list(Long requestedOrganizationId) {
        List<Refund> refunds;
        if (TenantContext.isPlatformStaff()) {
            refunds = requestedOrganizationId != null
                    ? refundRepository.findByPayment_Organization_Id(requestedOrganizationId)
                    : refundRepository.findAll();
        } else {
            Long ownOrganizationId = TenantContext.resolveOrganizationId(null);
            refunds = refundRepository.findByPayment_Organization_Id(ownOrganizationId);
        }
        return refunds.stream().map(RefundMapper::toResponse).toList();
    }

    public RefundResponse getById(Long id) {
        return RefundMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public RefundResponse create(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.paymentId()));
        if (!TenantContext.canAccess(payment.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payment not found: " + request.paymentId());
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(request.amount());
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.PENDING);

        return RefundMapper.toResponse(refundRepository.save(refund));
    }

    @Transactional
    public RefundResponse changeStatus(Long id, RefundStatusChangeRequest request) {
        TenantContext.requirePlatformStaff();
        Refund refund = findAccessibleEntity(id);

        refund.setStatus(request.status());
        refund.setProcessedAt(LocalDateTime.now());
        userRepository.findById(TenantContext.currentPrincipal().userId()).ifPresent(refund::setProcessedBy);

        if (request.status() == RefundStatus.PROCESSED) {
            Payment payment = refund.getPayment();
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.getInvoice().setStatus(InvoiceStatus.REFUNDED);
        }

        return RefundMapper.toResponse(refund);
    }

    private Refund findAccessibleEntity(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id));
        if (!TenantContext.canAccess(refund.getPayment().getOrganization().getId())) {
            throw new ResourceNotFoundException("Refund not found: " + id);
        }
        return refund;
    }
}
