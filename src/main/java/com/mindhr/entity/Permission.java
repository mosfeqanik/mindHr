package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import com.mindhr.entity.enums.PermissionAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"module", "action"}))
public class Permission extends BaseEntity {

    /** Target module, e.g. PAYMENT, INVOICE, MENU, CONTRACT. */
    @Column(nullable = false, length = 50)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PermissionAction action;
}
