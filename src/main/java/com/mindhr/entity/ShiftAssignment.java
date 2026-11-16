package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** One roster cell: an employee's shift (or day off) on a date. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_assignments", indexes = @Index(name = "idx_shift_assignments_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "work_date"})})
public class ShiftAssignment extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Null means the employee is off that day. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "shift_id", nullable = true)
    private Shift shift;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

}
