package com.mindhr.security;

import com.mindhr.entity.enums.UserType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves and enforces the {@code User -> Organization -> Data} isolation rule for
 * {@code Tenant} resources, using the {@link JwtAuthenticatedPrincipal} set by
 * {@code JwtAuthenticationFilter}.
 *
 * <p>{@code SUPER_ADMIN} and {@code SAAS_STAFF} are not bound to an organization and may
 * operate across all of them (e.g. Finance staff billing support); an {@code ORG_USER} is
 * always confined to their own {@code organizationId}, regardless of what a request asks for.
 */
public final class TenantContext {

    /** mindHR's own organization (set at startup by {@code InternalOrganizationProvisioner}). */
    private static volatile Long internalOrganizationId;

    private TenantContext() {
    }

    public static void setInternalOrganizationId(Long id) {
        internalOrganizationId = id;
    }

    public static Long internalOrganizationId() {
        return internalOrganizationId;
    }

    public static boolean isInternalOrganization(Long organizationId) {
        return organizationId != null && organizationId.equals(internalOrganizationId);
    }

    /**
     * The organization an HR-module request works on. Platform users (Super Admin, Admin, mindHR Staff) only ever
     * work on mindHR's own staff, so they always get the internal organization, whatever the request asks for;
     * client HR data stays private. An organization user gets their own organization.
     */
    public static Long resolveHrOrganizationId(Long requestedOrganizationId) {
        if (isPlatformStaff()) {
            if (internalOrganizationId == null) {
                throw new IllegalStateException("mindHR's internal organization is not set up yet");
            }
            return internalOrganizationId;
        }
        return resolveOrganizationId(requestedOrganizationId);
    }

    /** Whether the caller may read/modify an HR record owned by {@code entityOrganizationId}. */
    public static boolean canAccessHr(Long entityOrganizationId) {
        if (isPlatformStaff()) {
            return isInternalOrganization(entityOrganizationId);
        }
        return canAccess(entityOrganizationId);
    }

    public static JwtAuthenticatedPrincipal currentPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        throw new IllegalStateException("No authenticated JWT principal in the security context");
    }

    public static boolean isPlatformStaff() {
        UserType userType = currentPrincipal().userType();
        return userType == UserType.SUPER_ADMIN || userType == UserType.ADMIN || userType == UserType.SAAS_STAFF;
    }

    /**
     * Resolves which organization a list/create request should operate against. Platform staff
     * must supply {@code requestedOrganizationId} explicitly; an {@code ORG_USER}'s own
     * organization is used unconditionally, ignoring anything the request asked for.
     */
    public static Long resolveOrganizationId(Long requestedOrganizationId) {
        JwtAuthenticatedPrincipal principal = currentPrincipal();

        if (isPlatformStaff()) {
            if (requestedOrganizationId == null) {
                throw new IllegalArgumentException("organizationId is required");
            }
            return requestedOrganizationId;
        }

        if (principal.organizationId() == null) {
            throw new IllegalStateException("Organization user has no organizationId");
        }
        return principal.organizationId();
    }

    /** Whether the caller may read/modify a record owned by {@code entityOrganizationId}. */
    public static boolean canAccess(Long entityOrganizationId) {
        if (isPlatformStaff()) {
            return true;
        }
        Long ownOrganizationId = currentPrincipal().organizationId();
        return ownOrganizationId != null && ownOrganizationId.equals(entityOrganizationId);
    }

    /** For resources with no per-organization owner at all (Leads, platform roles, audit logs, ...). */
    public static void requirePlatformStaff() {
        if (!isPlatformStaff()) {
            throw new AccessDeniedException("This operation is restricted to SaaS platform staff");
        }
    }

    /** Managing user accounts: the Super Admin or an Admin. */
    public static void requireUserManager() {
        UserType userType = currentPrincipal().userType();
        if (userType != UserType.SUPER_ADMIN && userType != UserType.ADMIN) {
            throw new AccessDeniedException("This operation is restricted to the Super Admin or an Admin");
        }
    }

    public static boolean isSuperAdmin() {
        return currentPrincipal().userType() == UserType.SUPER_ADMIN;
    }

    /** For sensitive platform actions (managing other platform accounts, system roles, ...). */
    public static void requireSuperAdmin() {
        if (currentPrincipal().userType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("This operation is restricted to the Super Admin");
        }
    }
}
