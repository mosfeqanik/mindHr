package com.mindhr.security;

import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Used by the {@code AuthenticationManager} during login (via {@code DaoAuthenticationProvider}).
 * Requests that are already authenticated via a JWT do not go through this class —
 * {@link JwtAuthenticationFilter} trusts the signed token's claims instead of reloading
 * the user on every request.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailFetchingRolesAndPermissions(email)
                .map(user -> CustomUserDetails.fromUser(user,
                        employeeRepository.findByUserId(user.getId()).orElse(null)))
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }
}
