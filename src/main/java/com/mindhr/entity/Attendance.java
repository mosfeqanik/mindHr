package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Daily attendance. Employees check in/out through AttendanceActionController; generic writes are HR-only. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_records", indexes = @Index(name = "idx_attendance_records_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "work_date"})})
public class Attendance extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at", nullable = true)
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at", nullable = true)
    private LocalDateTime checkOutAt;

    @Column(name = "check_in_latitude", nullable = true)
    private Double checkInLatitude;

    @Column(name = "check_in_longitude", nullable = true)
    private Double checkInLongitude;

    @Column(name = "check_out_latitude", nullable = true)
    private Double checkOutLatitude;

    @Column(name = "check_out_longitude", nullable = true)
    private Double checkOutLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_method", nullable = true, length = 30)
    private AttendanceMethod method = AttendanceMethod.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes = 0;

    @Column(name = "worked_minutes", nullable = false)
    private int workedMinutes = 0;

    @Column(name = "note", nullable = true, length = 500)
    private String note;

}
