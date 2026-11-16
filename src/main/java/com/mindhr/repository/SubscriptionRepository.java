package com.mindhr.repository;

import com.mindhr.entity.Subscription;
import com.mindhr.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByOrganizationId(Long organizationId);

    List<Subscription> findByOrganizationIdAndStatusIn(Long organizationId, Collection<SubscriptionStatus> statuses);
}
