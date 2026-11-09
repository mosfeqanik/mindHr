package com.mindhr.service;

import com.mindhr.dto.SubscriptionPaymentQuote;
import com.mindhr.dto.SubscriptionPaymentRequest;
import com.mindhr.dto.SubscriptionPaymentResponse;
import com.mindhr.entity.Invoice;
import com.mindhr.entity.Payment;
import com.mindhr.entity.Receipt;
import com.mindhr.entity.Subscription;
import com.mindhr.entity.SubscriptionPlan;
import com.mindhr.entity.enums.BillingCycle;
import com.mindhr.entity.enums.CollectionChannel;
import com.mindhr.entity.enums.InvoiceStatus;
import com.mindhr.entity.enums.PaymentStatus;
import com.mindhr.entity.enums.SubscriptionStatus;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.InvoiceMapper;
import com.mindhr.mapper.PaymentMapper;
import com.mindhr.mapper.ReceiptMapper;
import com.mindhr.mapper.SubscriptionMapper;
import com.mindhr.repository.InvoiceRepository;
import com.mindhr.repository.PaymentRepository;
import com.mindhr.repository.ReceiptRepository;
import com.mindhr.repository.SubscriptionRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Platform staff record a one-month payment (cash, bKash or card) received from an organization for
 * its current subscription plan. One call, one transaction: a PAID invoice, a CONFIRMED payment and a
 * receipt are created, and the subscription's renewal date moves one month forward.
 *
 * <p>The month starts at the current renewal date if that is still in the future (paying ahead),
 * otherwise today. A YEARLY plan is charged price / 12 for the month.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionPaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionPaymentQuote quote(Long subscriptionId) {
        TenantContext.requirePlatformStaff();
        Subscription subscription = findSubscription(subscriptionId);
        LocalDate start = periodStart(subscription);
        SubscriptionPlan plan = subscription.getPlan();
        return new SubscriptionPaymentQuote(subscription.getId(), plan.getName(), monthlyAmount(plan),
                plan.getCurrency(), start, start.plusMonths(1));
    }

    @Transactional
    public SubscriptionPaymentResponse collect(Long subscriptionId, SubscriptionPaymentRequest request) {
        TenantContext.requirePlatformStaff();
        Subscription subscription = findSubscription(subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Subscription is cancelled; reactivate it before taking a payment");
        }

        String transactionId = blankToNull(request.transactionId());
        if (request.method() == CollectionChannel.BKASH && transactionId == null) {
            throw new IllegalArgumentException("bKash transaction id (TrxID) is required");
        }
        if (transactionId != null && paymentRepository.existsByGatewayTransactionIdIgnoreCase(transactionId)) {
            throw new DuplicateResourceException("A payment with transaction id '" + transactionId + "' already exists");
        }

        SubscriptionPlan plan = subscription.getPlan();
        BigDecimal amount = monthlyAmount(plan);
        String currency = plan.getCurrency();
        LocalDate periodStart = periodStart(subscription);
        LocalDate periodEnd = periodStart.plusMonths(1);
        LocalDateTime now = LocalDateTime.now();
        String period = periodStart + " to " + periodEnd;

        Invoice invoice = new Invoice();
        invoice.setOrganization(subscription.getOrganization());
        invoice.setSubscription(subscription);
        // The number is built from the generated id, so save once with a unique placeholder first.
        invoice.setInvoiceNumber("TMP-" + UUID.randomUUID().toString().substring(0, 26));
        invoice.setAmount(amount);
        invoice.setCurrency(currency);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setIssueDate(now.toLocalDate());
        invoice.setDueDate(now.toLocalDate());
        invoice.setPaidAt(now);
        invoice.setNotes(plan.getName() + ", 1 month (" + period + "), paid by " + request.method()
                + (request.notes() != null && !request.notes().isBlank() ? ". " + request.notes().trim() : ""));
        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber("SUB-INV-" + String.format("%08d", invoice.getId()));

        Payment payment = new Payment();
        payment.setOrganization(subscription.getOrganization());
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setGateway(request.method().name());
        payment.setGatewayTransactionId(transactionId);
        payment.setPaidAt(now);
        payment = paymentRepository.save(payment);

        Receipt receipt = new Receipt();
        receipt.setOrganization(subscription.getOrganization());
        receipt.setPayment(payment);
        receipt.setReceiptNumber("RCPT-" + String.format("%08d", payment.getId()));
        receipt.setAmount(amount);
        receipt.setIssuedAt(now);
        receipt = receiptRepository.save(receipt);
        payment.setReceipt(receipt);

        SubscriptionStatus fromStatus = subscription.getStatus();
        subscription.setRenewalDate(periodEnd);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionService.recordHistory(subscription, fromStatus, SubscriptionStatus.ACTIVE, plan, plan,
                "Payment " + invoice.getInvoiceNumber() + " by " + request.method() + ": " + amount + " " + currency
                        + " for " + period);

        return new SubscriptionPaymentResponse(SubscriptionMapper.toResponse(subscription),
                InvoiceMapper.toResponse(invoice), PaymentMapper.toResponse(payment),
                ReceiptMapper.toResponse(receipt), periodStart, periodEnd);
    }

    static BigDecimal monthlyAmount(SubscriptionPlan plan) {
        BigDecimal price = plan.getPrice();
        if (plan.getBillingCycle() == BillingCycle.YEARLY) {
            return price.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate periodStart(Subscription subscription) {
        LocalDate today = LocalDate.now();
        LocalDate paidUntil = subscription.getRenewalDate();
        return paidUntil != null && paidUntil.isAfter(today) ? paidUntil : today;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Subscription findSubscription(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    }
}
