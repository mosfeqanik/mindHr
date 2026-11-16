package com.mindhr.entity.base;

import com.mindhr.entity.Organization;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for every organization-owned record. Enforces the
 * User -> Organization -> Data isolation rule by requiring organization_id.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class Tenant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
