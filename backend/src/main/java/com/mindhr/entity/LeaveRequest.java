package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Applied and decided through LeaveRequestActionController. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leave_requests", indexes = @Index(name = "idx_leave_requests_organization", columnList = "organization_id"))
public class LeaveRequest extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "days", nullable = false, precision = 12, scale = 2)
    private BigDecimal days;

    @Column(name = "reason", nullable = true, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private Employee decidedBy;

    @Column(name = "decided_at", nullable = true)
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", nullable = true, length = 500)
    private String decisionNote;

}
