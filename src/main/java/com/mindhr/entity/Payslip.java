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
@Table(name = "payslips", indexes = @Index(name = "idx_payslips_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"payroll_run_id", "employee_id"})})
public class Payslip extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "basic_salary", nullable = true, precision = 12, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "allowances", nullable = true, precision = 12, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "overtime", nullable = true, precision = 12, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "bonus", nullable = true, precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "commission", nullable = true, precision = 12, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = true, precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "late_deduction", nullable = true, precision = 12, scale = 2)
    private BigDecimal lateDeduction = BigDecimal.ZERO;

    @Column(name = "absent_deduction", nullable = true, precision = 12, scale = 2)
    private BigDecimal absentDeduction = BigDecimal.ZERO;

    @Column(name = "loan_deduction", nullable = true, precision = 12, scale = 2)
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @Column(name = "advance_deduction", nullable = true, precision = 12, scale = 2)
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = true, precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "days_present", nullable = false)
    private int daysPresent = 0;

    @Column(name = "days_absent", nullable = false)
    private int daysAbsent = 0;

    @Column(name = "days_late", nullable = false)
    private int daysLate = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private PayrollStatus status = PayrollStatus.DRAFT;

}
