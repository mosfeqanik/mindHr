package com.mindhr.repository;

import com.mindhr.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrganizationIdAndUserIdOrderByIdDesc(Long organizationId, Long userId);

    List<Notification> findByOrganizationIdOrderByIdDesc(Long organizationId);
}
