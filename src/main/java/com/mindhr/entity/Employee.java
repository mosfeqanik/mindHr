package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.EmploymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees",
        indexes = @Index(name = "idx_employees_organization", columnList = "organization_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "employee_code"}))
public class Employee extends Tenant {

    /**
     * Login account used by the mobile app. Always set for employees created through the API; the
     * column stays nullable only so rows created before this rule keep working.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Human-readable unique ID within the organization, e.g. EMP-0007. */
    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    /** Job title / designation. */
    @Column(length = 100)
    private String position;

    @Column(name = "national_id_serial", length = 100)
    private String nationalIdSerial;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 255)
    private String address;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Direct manager; drives the TEAM data scope (a manager sees their direct reports). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @Column(nullable = false)
    private boolean active = true;
}
