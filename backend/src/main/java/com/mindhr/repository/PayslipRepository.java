package com.mindhr.repository;

import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    List<Payslip> findByPayrollRunId(Long payrollRunId);
}
