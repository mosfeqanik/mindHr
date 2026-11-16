package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Employee timeline (joined, salary revised, promotion, ...). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee_events", indexes = @Index(name = "idx_employee_events_organization", columnList = "organization_id"))
public class EmployeeEvent extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EmployeeEventType type;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "details", nullable = true, columnDefinition = "text")
    private String details;

    @Column(name = "old_value", nullable = true, length = 255)
    private String oldValue;

    @Column(name = "new_value", nullable = true, length = 255)
    private String newValue;

}
