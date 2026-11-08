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
@Table(name = "employee_payment_accounts", indexes = @Index(name = "idx_employee_payment_accounts_organization", columnList = "organization_id"))
public class EmployeePaymentAccount extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private PaymentAccountType type;

    /** Bank name or mobile wallet provider. */
    @Column(name = "provider", nullable = true, length = 100)
    private String provider;

    @Column(name = "account_name", nullable = true, length = 150)
    private String accountName;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryAccount = false;

}
