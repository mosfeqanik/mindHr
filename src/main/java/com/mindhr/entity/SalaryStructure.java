package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Salary history; the row with the latest effectiveFrom not after today is the current one. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "salary_structures", indexes = @Index(name = "idx_salary_structures_organization", columnList = "organization_id"))
public class SalaryStructure extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "allowances", nullable = true, precision = 12, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "currency", nullable = true, length = 3)
    private String currency = "BDT";

}
