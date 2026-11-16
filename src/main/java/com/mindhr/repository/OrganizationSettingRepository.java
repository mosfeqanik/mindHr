package com.mindhr.repository;

import com.mindhr.entity.OrganizationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationSettingRepository extends JpaRepository<OrganizationSetting, Long> {

    List<OrganizationSetting> findByOrganizationId(Long organizationId);

    Optional<OrganizationSetting> findByOrganizationIdAndKeyIgnoreCase(Long organizationId, String key);

    boolean existsByOrganizationIdAndKeyIgnoreCase(Long organizationId, String key);

    boolean existsByOrganizationIdAndKeyIgnoreCaseAndIdNot(Long organizationId, String key, Long id);
}
