package com.mindhr.service;

import com.mindhr.config.JwtProperties;
import com.mindhr.dto.ChangePasswordRequest;
import com.mindhr.dto.LoginRequest;
import com.mindhr.dto.LoginResponse;
import com.mindhr.entity.User;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.CustomUserDetails;
import com.mindhr.security.JwtService;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailFetchingRolesAndPermissions(request.email())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!user.isActive()) {
            throw new DisabledException("This account is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());

        CustomUserDetails principal = CustomUserDetails.fromUser(user,
                employeeRepository.findByUserId(user.getId()).orElse(null));
        String token = jwtService.generateToken(principal);

        return new LoginResponse(
                token,
                "Bearer",
                jwtProperties.expirationMs() / 1000,
                user.getFullName(),
                user.getUserType().name()
        );
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = TenantContext.currentPrincipal().userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
