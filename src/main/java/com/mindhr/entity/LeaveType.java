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
@Table(name = "leave_types", indexes = @Index(name = "idx_leave_types_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "name"})})
public class LeaveType extends Tenant {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "annual_days", nullable = false)
    private int annualDays = 0;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

}
