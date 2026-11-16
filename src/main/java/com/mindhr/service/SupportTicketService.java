package com.mindhr.service;

import com.mindhr.dto.SupportTicketAssignRequest;
import com.mindhr.dto.SupportTicketRequest;
import com.mindhr.dto.SupportTicketResponse;
import com.mindhr.dto.SupportTicketStatusChangeRequest;
import com.mindhr.entity.Organization;
import com.mindhr.entity.SupportTicket;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.TicketStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.SupportTicketMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.SupportTicketRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public List<SupportTicketResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return supportTicketRepository.findByOrganizationId(organizationId).stream()
                .map(SupportTicketMapper::toResponse)
                .toList();
    }

    public SupportTicketResponse getById(Long id) {
        return SupportTicketMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public SupportTicketResponse create(SupportTicketRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        User createdBy = userRepository.findById(TenantContext.currentPrincipal().userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + TenantContext.currentPrincipal().userId()));

        SupportTicket ticket = new SupportTicket();
        ticket.setOrganization(organization);
        ticket.setCreatedBy(createdBy);
        ticket.setTicketNumber("TCK-" + System.currentTimeMillis());
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setType(request.type());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);

        return SupportTicketMapper.toResponse(supportTicketRepository.save(ticket));
    }

    @Transactional
    public SupportTicketResponse update(Long id, SupportTicketRequest request) {
        SupportTicket ticket = findAccessibleEntity(id);
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setType(request.type());
        ticket.setPriority(request.priority());
        return SupportTicketMapper.toResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse changeStatus(Long id, SupportTicketStatusChangeRequest request) {
        SupportTicket ticket = findAccessibleEntity(id);
        ticket.setStatus(request.status());
        LocalDateTime now = LocalDateTime.now();
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        } else if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(now);
        }
        return SupportTicketMapper.toResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse assign(Long id, SupportTicketAssignRequest request) {
        TenantContext.requirePlatformStaff();
        SupportTicket ticket = findAccessibleEntity(id);
        User assignee = userRepository.findById(request.assignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.assignedToUserId()));
        ticket.setAssignedTo(assignee);
        return SupportTicketMapper.toResponse(ticket);
    }

    @Transactional
    public void delete(Long id) {
        TenantContext.requirePlatformStaff();
        SupportTicket ticket = findAccessibleEntity(id);
        supportTicketRepository.delete(ticket);
    }

    private SupportTicket findAccessibleEntity(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + id));
        if (!TenantContext.canAccess(ticket.getOrganization().getId())) {
            throw new ResourceNotFoundException("Support ticket not found: " + id);
        }
        return ticket;
    }
}
