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
@Table(name = "assets", indexes = @Index(name = "idx_assets_organization", columnList = "organization_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "asset_tag"})})
public class Asset extends Tenant {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "asset_tag", nullable = false, length = 100)
    private String assetTag;

    @Column(name = "serial_number", nullable = true, length = 100)
    private String serialNumber;

    @Column(name = "category", nullable = true, length = 100)
    private String category;

    @Column(name = "description", nullable = true, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 30)
    private AssetStatus status = AssetStatus.AVAILABLE;

}
