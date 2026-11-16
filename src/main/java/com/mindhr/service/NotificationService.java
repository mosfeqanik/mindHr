package com.mindhr.service;

import com.mindhr.dto.NotificationRequest;
import com.mindhr.dto.NotificationResponse;
import com.mindhr.entity.Notification;
import com.mindhr.entity.Organization;
import com.mindhr.entity.User;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.NotificationMapper;
import com.mindhr.repository.NotificationRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A notification is personal: an {@code ORG_USER} only ever sees their own inbox (never their
 * whole organization's), while platform staff can inspect any user's for support purposes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public List<NotificationResponse> list(Long requestedOrganizationId, Long requestedUserId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);

        List<Notification> notifications;
        if (TenantContext.isPlatformStaff()) {
            notifications = requestedUserId != null
                    ? notificationRepository.findByOrganizationIdAndUserIdOrderByIdDesc(organizationId, requestedUserId)
                    : notificationRepository.findByOrganizationIdOrderByIdDesc(organizationId);
        } else {
            Long ownUserId = TenantContext.currentPrincipal().userId();
            notifications = notificationRepository.findByOrganizationIdAndUserIdOrderByIdDesc(organizationId, ownUserId);
        }
        return notifications.stream().map(NotificationMapper::toResponse).toList();
    }

    public NotificationResponse getById(Long id) {
        return NotificationMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        User recipient = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        if (recipient.getOrganization() == null || !recipient.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Recipient does not belong to this organization");
        }

        Notification notification = new Notification();
        notification.setOrganization(organization);
        notification.setUser(recipient);
        notification.setChannel(request.channel());
        notification.setSubject(request.subject());
        notification.setBody(request.body());
        notification.setRead(false);

        return NotificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse markSent(Long id) {
        Notification notification = findAccessibleEntity(id);
        notification.setSentAt(LocalDateTime.now());
        return NotificationMapper.toResponse(notification);
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = findAccessibleEntity(id);
        notification.setRead(true);
        return NotificationMapper.toResponse(notification);
    }

    @Transactional
    public void delete(Long id) {
        Notification notification = findAccessibleEntity(id);
        notificationRepository.delete(notification);
    }

    private Notification findAccessibleEntity(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        boolean isRecipient = notification.getUser().getId().equals(TenantContext.currentPrincipal().userId());
        if (!isRecipient && !TenantContext.isPlatformStaff()) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        return notification;
    }
}
