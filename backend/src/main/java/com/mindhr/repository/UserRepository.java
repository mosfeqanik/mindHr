package com.mindhr.repository;

import com.mindhr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByOrganizationId(Long organizationId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Loads a user together with their roles and each role's permissions in a single
     * query, so authorities can be built for the JWT without hitting the lazy
     * {@code roles} / {@code roles.permissions} collections outside a transaction.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.email = :email
            """)
    Optional<User> findByEmailFetchingRolesAndPermissions(@Param("email") String email);
}
