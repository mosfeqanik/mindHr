package com.mindhr.security;

import com.mindhr.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/** Issues and validates the app's own JWTs — this service is both the issuer and the verifier. */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        // jwt.secret is expected to be Base64-encoded (>= 32 raw bytes for HS256) — see JwtProperties.
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String generateToken(CustomUserDetails principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.getId())
                .claim("userType", principal.getUserType().name())
                .claim("organizationId", principal.getOrganizationId())
                .claim("employeeId", principal.getEmployeeId())
                .claim("branchId", principal.getBranchId())
                .claim("dataScope", principal.getDataScope().name())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    /** Parses and signature/expiry-validates the token, returning its claims. Throws {@link JwtException} if invalid. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
