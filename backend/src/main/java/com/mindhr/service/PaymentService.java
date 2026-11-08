package com.mindhr.service;

import com.mindhr.dto.PaymentRequest;
import com.mindhr.dto.PaymentResponse;
import com.mindhr.dto.PaymentStatusChangeRequest;
import com.mindhr.dto.ReceiptResponse;
import com.mindhr.entity.Invoice;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Payment;
import com.mindhr.entity.PaymentMethod;
import com.mindhr.entity.Receipt;
import com.mindhr.entity.enums.InvoiceStatus;
import com.mindhr.entity.enums.PaymentStatus;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.PaymentMapper;
import com.mindhr.mapper.ReceiptMapper;
import com.mindhr.repository.InvoiceRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.PaymentMethodRepository;
import com.mindhr.repository.PaymentRepository;
import com.mindhr.repository.ReceiptRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** No delete: a payment is a financial record. Confirming one also marks its invoice PAID. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrganizationRepository organizationRepository;

    public List<PaymentResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return paymentRepository.findByOrganizationId(organizationId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    public PaymentResponse getById(Long id) {
        return PaymentMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + request.invoiceId()));
        if (!invoice.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Invoice does not belong to this organization");
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(request.paymentMethodId(), organizationId);

        Payment payment = new Payment();
        payment.setOrganization(organization);
        payment.setInvoice(invoice);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency() != null ? request.currency().toUpperCase() : "USD");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGateway(request.gateway());
        payment.setGatewayTransactionId(request.gatewayTransactionId());

        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse changeStatus(Long id, PaymentStatusChangeRequest request) {
        Payment payment = findAccessibleEntity(id);
        payment.setStatus(request.status());

        if (request.status() == PaymentStatus.CONFIRMED) {
            payment.setPaidAt(LocalDateTime.now());
            Invoice invoice = payment.getInvoice();
            invoice.setStatus(InvoiceStatus.PAID);
            if (invoice.getPaidAt() == null) {
                invoice.setPaidAt(payment.getPaidAt());
            }
        }

        return PaymentMapper.toResponse(payment);
    }

    @Transactional
    public ReceiptResponse issueReceipt(Long id) {
        Payment payment = findAccessibleEntity(id);

        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new IllegalArgumentException("A receipt can only be issued for a confirmed payment");
        }
        if (receiptRepository.findByPaymentId(id).isPresent()) {
            throw new DuplicateResourceException("A receipt has already been issued for this payment");
        }

        Receipt receipt = new Receipt();
        receipt.setOrganization(payment.getOrganization());
        receipt.setPayment(payment);
        receipt.setReceiptNumber("RCPT-" + String.format("%08d", payment.getId()));
        receipt.setAmount(payment.getAmount());
        receipt.setIssuedAt(LocalDateTime.now());

        return ReceiptMapper.toResponse(receiptRepository.save(receipt));
    }

    private PaymentMethod resolvePaymentMethod(Long paymentMethodId, Long organizationId) {
        if (paymentMethodId == null) {
            return null;
        }
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + paymentMethodId));
        if (!paymentMethod.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Payment method does not belong to this organization");
        }
        return paymentMethod;
    }

    private Payment findAccessibleEntity(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        if (!TenantContext.canAccess(payment.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payment not found: " + id);
        }
        return payment;
    }
}
