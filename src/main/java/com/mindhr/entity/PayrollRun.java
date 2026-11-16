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
@Table(name = "payroll_runs", indexes = @Index(name = "idx_payroll_runs_organization", columnList = "organization_id"))
public class PayrollRun extends Tenant {

    @Column(name = "run_year", nullable = false)
    private int year = 0;

    @Column(name = "run_month", nullable = false)
    private int month = 0;

    /** Null runs payroll for the whole organization. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "branch_id", nullable = true)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "note", nullable = true, length = 500)
    private String note;

}
