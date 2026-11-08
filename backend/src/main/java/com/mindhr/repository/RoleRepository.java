package com.mindhr.repository;

import com.mindhr.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByOrganizationIdIsNull();

    List<Role> findByOrganizationIdOrOrganizationIdIsNull(Long organizationId);

    java.util.Optional<Role> findByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(Long organizationId, String name, Long id);

    boolean existsByOrganizationIdIsNullAndNameIgnoreCase(String name);

    boolean existsByOrganizationIdIsNullAndNameIgnoreCaseAndIdNot(String name, Long id);
}
