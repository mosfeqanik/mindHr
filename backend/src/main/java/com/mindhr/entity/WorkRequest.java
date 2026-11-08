package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Employee request (advance, loan, shift change, expense, ...) approved by a manager. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_requests", indexes = @Index(name = "idx_work_requests_organization", columnList = "organization_id"))
public class WorkRequest extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "details", nullable = true, columnDefinition = "text")
    private String details;

    @Column(name = "amount", nullable = true, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "request_date", nullable = true)
    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private Employee decidedBy;

    @Column(name = "decided_at", nullable = true)
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", nullable = true, length = 500)
    private String decisionNote;

}
