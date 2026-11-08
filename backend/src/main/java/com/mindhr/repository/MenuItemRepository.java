package com.mindhr.repository;

import com.mindhr.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByOrganizationId(Long organizationId);

    List<MenuItem> findByCategoryId(Long categoryId);
}
