package com.mindhr.repository;

import com.mindhr.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByKeyIgnoreCase(String key);

    boolean existsByKeyIgnoreCase(String key);

    boolean existsByKeyIgnoreCaseAndIdNot(String key, Long id);
}
