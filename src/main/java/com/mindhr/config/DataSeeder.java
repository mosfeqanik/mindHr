package com.mindhr.config;

import com.mindhr.entity.Permission;
import com.mindhr.entity.Role;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.PermissionAction;
import com.mindhr.entity.enums.RoleScope;
import com.mindhr.entity.enums.UserType;
import com.mindhr.repository.PermissionRepository;
import com.mindhr.repository.RoleRepository;
import com.mindhr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Idempotent first-run seed: creates one SUPER_ADMIN user, a starter set of {@link Permission}
 * rows, and a platform {@link Role} carrying them — the minimum needed to exercise login. Skips
 * entirely if any user already exists. Guarded by {@code seed.enabled} (default {@code true}).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Platform-level modules seeded with every {@link PermissionAction} for the initial admin role. */
    private static final List<String> SEED_MODULES = List.of("USER", "ORGANIZATION");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email}")
    private String adminEmail;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.warn("No users found — seeding initial SUPER_ADMIN account ({}). " +
                "Change its password immediately; it is set from the seed.admin.password property.", adminEmail);

        List<Permission> permissions = new ArrayList<>();
        for (String module : SEED_MODULES) {
            for (PermissionAction action : PermissionAction.values()) {
                Permission permission = new Permission();
                permission.setModule(module);
                permission.setAction(action);
                permissions.add(permissionRepository.save(permission));
            }
        }

        Role adminRole = new Role();
        adminRole.setName("SUPER_ADMIN");
        adminRole.setScope(RoleScope.PLATFORM);
        adminRole.setSystem(true);
        adminRole.setPermissions(new HashSet<>(permissions));
        adminRole = roleRepository.save(adminRole);

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("Super Admin");
        admin.setUserType(UserType.SUPER_ADMIN);
        admin.setActive(true);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
    }
}
