package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit trail. organization_id is null for platform-level actions. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activity_logs", indexes = @Index(name = "idx_activity_logs_organization", columnList = "organization_id"))
public class ActivityLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** JSON payload describing the change. */
    @Column(columnDefinition = "text")
    private String details;

    /** 45 chars fits a full IPv6 address. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
