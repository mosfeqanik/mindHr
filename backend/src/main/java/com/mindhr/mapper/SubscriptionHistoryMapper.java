package com.mindhr.mapper;

import com.mindhr.dto.SubscriptionHistoryResponse;
import com.mindhr.entity.SubscriptionHistory;

public final class SubscriptionHistoryMapper {

    private SubscriptionHistoryMapper() {
    }

    public static SubscriptionHistoryResponse toResponse(SubscriptionHistory history) {
        return new SubscriptionHistoryResponse(
                history.getId(),
                history.getSubscription().getId(),
                history.getFromPlan() != null ? history.getFromPlan().getId() : null,
                history.getFromPlan() != null ? history.getFromPlan().getName() : null,
                history.getToPlan() != null ? history.getToPlan().getId() : null,
                history.getToPlan() != null ? history.getToPlan().getName() : null,
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : null,
                history.getNote(),
                history.getChangedAt()
        );
    }
}
