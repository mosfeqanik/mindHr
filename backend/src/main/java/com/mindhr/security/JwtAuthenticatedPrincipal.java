package com.mindhr.security;

import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.UserType;

/**
 * Lightweight principal set on the {@code SecurityContext} for requests authenticated via a
 * validated JWT. Built entirely from the token's claims — no database lookup is performed
 * per request; the signed token is trusted as-is until it expires.
 */
public record JwtAuthenticatedPrincipal(Long userId, String email, UserType userType, Long organizationId,
                                        Long employeeId, Long branchId, DataScope dataScope) {
}
