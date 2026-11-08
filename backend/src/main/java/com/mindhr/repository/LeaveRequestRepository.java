package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    List<LeaveRequest> findByEmployeeIdAndStatusIn(Long employeeId, java.util.Collection<LeaveStatus> statuses);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);
}
