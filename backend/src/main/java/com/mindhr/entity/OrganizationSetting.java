package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "setting_key"}))
public class OrganizationSetting extends Tenant {

    @Column(name = "setting_key", nullable = false, length = 100)
    private String key;

    @Column(name = "setting_value", columnDefinition = "text")
    private String value;
}
