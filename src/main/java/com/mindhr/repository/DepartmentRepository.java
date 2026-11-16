package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
