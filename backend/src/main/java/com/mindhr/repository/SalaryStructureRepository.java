package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    List<SalaryStructure> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
}
