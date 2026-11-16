package com.mindhr.service;

import com.mindhr.dto.LeadRequest;
import com.mindhr.dto.LeadResponse;
import com.mindhr.entity.Lead;
import com.mindhr.entity.Organization;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.LeadStage;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.LeadMapper;
import com.mindhr.repository.LeadRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Pre-organization sales pipeline: entirely SaaS-side (Sales staff / Super Admin), never visible to an ORG_USER. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public List<LeadResponse> listAll() {
        TenantContext.requirePlatformStaff();
        return leadRepository.findAll().stream().map(LeadMapper::toResponse).toList();
    }

    public LeadResponse getById(Long id) {
        TenantContext.requirePlatformStaff();
        return LeadMapper.toResponse(findEntity(id));
    }

    @Transactional
    public LeadResponse create(LeadRequest request) {
        TenantContext.requirePlatformStaff();

        Lead lead = new Lead();
        lead.setAssignedTo(resolveUser(request.assignedToUserId()));
        applyRequest(lead, request);
        return LeadMapper.toResponse(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse update(Long id, LeadRequest request) {
        TenantContext.requirePlatformStaff();
        Lead lead = findEntity(id);
        lead.setAssignedTo(resolveUser(request.assignedToUserId()));
        applyRequest(lead, request);
        return LeadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse convert(Long id, Long organizationId) {
        TenantContext.requirePlatformStaff();
        Lead lead = findEntity(id);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        lead.setConvertedOrganization(organization);
        lead.setStage(LeadStage.CONVERTED);
        return LeadMapper.toResponse(lead);
    }

    @Transactional
    public void delete(Long id) {
        TenantContext.requirePlatformStaff();
        if (!leadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lead not found: " + id);
        }
        leadRepository.deleteById(id);
    }

    private void applyRequest(Lead lead, LeadRequest request) {
        lead.setCompanyName(request.companyName());
        lead.setContactName(request.contactName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setStage(request.stage());
        lead.setSource(request.source());
        lead.setNotes(request.notes());
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Lead findEntity(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }
}
