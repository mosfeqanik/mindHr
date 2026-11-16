package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    Optional<ShiftAssignment> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
}
