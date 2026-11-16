package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leave_balances", indexes = @Index(name = "idx_leave_balances_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "leave_year"})})
public class LeaveBalance extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "leave_year", nullable = false)
    private int year = 0;

    @Column(name = "entitled_days", nullable = false, precision = 12, scale = 2)
    private BigDecimal entitledDays;

    @Column(name = "used_days", nullable = true, precision = 12, scale = 2)
    private BigDecimal usedDays = BigDecimal.ZERO;

}
