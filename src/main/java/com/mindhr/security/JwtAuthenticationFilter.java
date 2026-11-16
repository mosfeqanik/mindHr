package com.mindhr.security;

import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates the JWT, and — if valid —
 * populates the {@code SecurityContext} with a {@link JwtAuthenticatedPrincipal} and the
 * authorities carried in the token's {@code authorities} claim.
 *
 * <p>An invalid/missing/expired token is not rejected here; the request simply proceeds
 * unauthenticated, and {@code SecurityConfig}'s {@code authorizeHttpRequests} rule plus
 * {@link AuthEntryPointJson} handle the 401 for endpoints that require authentication.
 *
 * <p>Deliberately <b>not</b> a {@code @Component}: it is constructed and wired explicitly into
 * the {@code SecurityFilterChain} by {@code SecurityConfig} via {@code addFilterBefore}. If this
 * were also a Spring-managed bean, Spring Boot's servlet {@code FilterRegistrationBean}
 * auto-configuration would register it a second time as a raw servlet filter, running it twice
 * per request.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parseClaims(token);
                authenticate(claims);
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token: leave the context unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private void authenticate(Claims claims) {
        Long userId = claims.get("userId", Long.class);
        String email = claims.getSubject();
        UserType userType = UserType.valueOf(claims.get("userType", String.class));
        Long organizationId = claims.get("organizationId", Long.class);
        Long employeeId = claims.get("employeeId", Long.class);
        Long branchId = claims.get("branchId", Long.class);
        String scopeClaim = claims.get("dataScope", String.class);
        DataScope dataScope = scopeClaim != null ? DataScope.valueOf(scopeClaim) : DataScope.ORGANIZATION;

        List<String> authorityClaims = claims.get("authorities", List.class);
        List<GrantedAuthority> authorities = authorityClaims == null
                ? List.of()
                : authorityClaims.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();

        JwtAuthenticatedPrincipal principal = new JwtAuthenticatedPrincipal(userId, email, userType, organizationId,
                employeeId, branchId, dataScope);

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
