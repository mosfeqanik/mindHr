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

    private TenantContext() {
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
        return userType == UserType.SUPER_ADMIN || userType == UserType.SAAS_STAFF;
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

    /** For sensitive platform actions (managing other platform accounts, system roles, ...). */
    public static void requireSuperAdmin() {
        if (currentPrincipal().userType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("This operation is restricted to the Super Admin");
        }
    }
}
