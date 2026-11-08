package com.mindhr.service;

import com.mindhr.dto.PaymentMethodRequest;
import com.mindhr.dto.PaymentMethodResponse;
import com.mindhr.entity.Organization;
import com.mindhr.entity.PaymentMethod;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.PaymentMethodMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.PaymentMethodRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final OrganizationRepository organizationRepository;

    public List<PaymentMethodResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return paymentMethodRepository.findByOrganizationId(organizationId).stream()
                .map(PaymentMethodMapper::toResponse)
                .toList();
    }

    public PaymentMethodResponse getById(Long id) {
        return PaymentMethodMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setOrganization(organization);
        applyRequest(paymentMethod, request);
        return PaymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public PaymentMethodResponse update(Long id, PaymentMethodRequest request) {
        PaymentMethod paymentMethod = findAccessibleEntity(id);
        applyRequest(paymentMethod, request);
        return PaymentMethodMapper.toResponse(paymentMethod);
    }

    @Transactional
    public void delete(Long id) {
        PaymentMethod paymentMethod = findAccessibleEntity(id);
        paymentMethodRepository.delete(paymentMethod);
    }

    private void applyRequest(PaymentMethod paymentMethod, PaymentMethodRequest request) {
        paymentMethod.setType(request.type());
        paymentMethod.setMaskedDetails(request.maskedDetails());
        paymentMethod.setGatewayToken(request.gatewayToken());

        boolean makeDefault = Boolean.TRUE.equals(request.defaultMethod());
        if (makeDefault) {
            clearExistingDefault(paymentMethod.getOrganization().getId(), paymentMethod.getId());
        }
        paymentMethod.setDefaultMethod(makeDefault);
    }

    /** Only one payment method per organization may be the default. */
    private void clearExistingDefault(Long organizationId, Long exceptId) {
        paymentMethodRepository.findByOrganizationIdAndDefaultMethodTrue(organizationId).stream()
                .filter(existing -> !existing.getId().equals(exceptId))
                .forEach(existing -> existing.setDefaultMethod(false));
    }

    private PaymentMethod findAccessibleEntity(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + id));
        if (!TenantContext.canAccess(paymentMethod.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payment method not found: " + id);
        }
        return paymentMethod;
    }
}
