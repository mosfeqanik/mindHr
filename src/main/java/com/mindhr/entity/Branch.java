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
@Table(name = "branches", indexes = @Index(name = "idx_branches_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "code"})})
public class Branch extends Tenant {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = true, length = 30)
    private String code;

    @Column(name = "address", nullable = true, length = 255)
    private String address;

    @Column(name = "phone", nullable = true, length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "manager_id", nullable = true)
    private Employee manager;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Geofence centre for GPS attendance. */
    @Column(name = "latitude", nullable = true)
    private Double latitude;

    @Column(name = "longitude", nullable = true)
    private Double longitude;

    /** Allowed check-in radius in metres; null disables the check. */
    @Column(name = "geofence_radius_m", nullable = true)
    private Integer geofenceRadiusM;

}
