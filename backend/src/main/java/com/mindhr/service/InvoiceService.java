package com.mindhr.service;

import com.mindhr.dto.InvoiceRequest;
import com.mindhr.dto.InvoiceResponse;
import com.mindhr.dto.InvoiceStatusChangeRequest;
import com.mindhr.entity.Invoice;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Subscription;
import com.mindhr.entity.enums.InvoiceStatus;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.InvoiceMapper;
import com.mindhr.repository.InvoiceRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.SubscriptionRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** No delete: an issued invoice is a financial record, voided via status rather than removed. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<InvoiceResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return invoiceRepository.findByOrganizationId(organizationId).stream()
                .map(InvoiceMapper::toResponse)
                .toList();
    }

    public InvoiceResponse getById(Long id) {
        return InvoiceMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());

        if (invoiceRepository.existsByInvoiceNumberIgnoreCase(request.invoiceNumber())) {
            throw new DuplicateResourceException("An invoice numbered '" + request.invoiceNumber() + "' already exists");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Subscription subscription = resolveSubscription(request.subscriptionId(), organizationId);

        Invoice invoice = new Invoice();
        invoice.setOrganization(organization);
        invoice.setSubscription(subscription);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setAmount(request.amount());
        invoice.setCurrency(request.currency() != null ? request.currency().toUpperCase() : "USD");
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());

        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        Invoice invoice = findAccessibleEntity(id);

        if (invoiceRepository.existsByInvoiceNumberIgnoreCaseAndIdNot(request.invoiceNumber(), id)) {
            throw new DuplicateResourceException("An invoice numbered '" + request.invoiceNumber() + "' already exists");
        }

        Subscription subscription = resolveSubscription(request.subscriptionId(), invoice.getOrganization().getId());
        invoice.setSubscription(subscription);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setAmount(request.amount());
        invoice.setCurrency(request.currency() != null ? request.currency().toUpperCase() : invoice.getCurrency());
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());

        return InvoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse changeStatus(Long id, InvoiceStatusChangeRequest request) {
        Invoice invoice = findAccessibleEntity(id);
        invoice.setStatus(request.status());
        if (request.status() == InvoiceStatus.PAID && invoice.getPaidAt() == null) {
            invoice.setPaidAt(LocalDateTime.now());
        }
        return InvoiceMapper.toResponse(invoice);
    }

    private Subscription resolveSubscription(Long subscriptionId, Long organizationId) {
        if (subscriptionId == null) {
            return null;
        }
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        if (!subscription.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Subscription does not belong to this organization");
        }
        return subscription;
    }

    private Invoice findAccessibleEntity(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (!TenantContext.canAccess(invoice.getOrganization().getId())) {
            throw new ResourceNotFoundException("Invoice not found: " + id);
        }
        return invoice;
    }
}
