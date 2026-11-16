package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);
}
