package com.mindhr.repository;

import com.mindhr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);

    Optional<Employee> findByUserId(Long userId);

    List<Employee> findByOrganizationIdAndBranchId(Long organizationId, Long branchId);

    List<Employee> findByManagerId(Long managerId);

    boolean existsByOrganizationIdAndEmployeeCodeIgnoreCase(Long organizationId, String employeeCode);

    boolean existsByOrganizationIdAndEmployeeCodeIgnoreCaseAndIdNot(Long organizationId, String employeeCode, Long id);

    /** Email is unique across all organizations (it is also the employee's login). */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Phone is unique within an organization. */
    boolean existsByOrganizationIdAndPhone(Long organizationId, String phone);

    boolean existsByOrganizationIdAndPhoneAndIdNot(Long organizationId, String phone, Long id);
}
