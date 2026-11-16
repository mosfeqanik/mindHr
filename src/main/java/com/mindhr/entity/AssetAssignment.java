package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

/** Assigned and returned through AssetAssignmentActionController. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_assignments", indexes = @Index(name = "idx_asset_assignments_organization", columnList = "organization_id"))
public class AssetAssignment extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "assigned_at", nullable = false)
    private LocalDate assignedAt;

    @Column(name = "returned_at", nullable = true)
    private LocalDate returnedAt;

    @Column(name = "condition_note", nullable = true, length = 500)
    private String conditionNote;

}
