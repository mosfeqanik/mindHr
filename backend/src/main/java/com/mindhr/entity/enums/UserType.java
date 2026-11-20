package com.mindhr.entity.enums;

public enum UserType {
    SUPER_ADMIN,
    /** Platform admin: platform staff who can also manage users (never Super Admin accounts). */
    ADMIN,
    SAAS_STAFF,
    ORG_USER
}
