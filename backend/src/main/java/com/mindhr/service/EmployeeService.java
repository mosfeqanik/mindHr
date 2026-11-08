package com.mindhr.service;

import com.mindhr.dto.EmployeeRequest;
import com.mindhr.dto.EmployeeResponse;
import com.mindhr.entity.Branch;
import com.mindhr.entity.Department;
import com.mindhr.entity.Employee;
import com.mindhr.entity.Organization;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.EmployeeEventType;
import com.mindhr.entity.enums.UserType;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.EmployeeMapper;
import com.mindhr.repository.BranchRepository;
import com.mindhr.repository.DepartmentRepository;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final PlanLimitService planLimitService;
    private final DataScopeService scope;
    private final EmployeeTimelineService eventService;
    private final OrgRoleProvisioner roleProvisioner;
    private final PasswordEncoder passwordEncoder;

    public List<EmployeeResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return employeeRepository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getId()))
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public EmployeeResponse getById(Long id) {
        return EmployeeMapper.toResponse(findAccessibleEntity(id));
    }

    /** The caller's own employee record (for the mobile app's profile screen). */
    public EmployeeResponse me() {
        Employee employee = employeeRepository.findById(scope.requireCurrentEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return EmployeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        planLimitService.assertCanAddEmployee(organizationId);
        assertUniqueContact(request, organizationId, null, null);

        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setUser(request.userId() != null
                ? resolveExistingUser(request.userId(), organizationId)
                : createLogin(request, organization));
        employee.setEmployeeCode(resolveEmployeeCode(request.employeeCode(), organizationId, null));
        applyRequest(employee, request, organizationId);
        Employee saved = employeeRepository.save(employee);

        LocalDate joined = saved.getJoinedAt() != null ? saved.getJoinedAt() : LocalDate.now();
        eventService.record(saved, EmployeeEventType.JOINED, joined, "Joined the company", null, saved.getPosition(), null);
        return EmployeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findAccessibleEntity(id);
        Long organizationId = employee.getOrganization().getId();

        String oldPosition = employee.getPosition();
        Long oldBranchId = idOf(employee.getBranch());
        Long oldDepartmentId = idOf(employee.getDepartment());

        if (request.userId() != null && !request.userId().equals(idOf(employee.getUser()))) {
            employee.setUser(resolveExistingUser(request.userId(), organizationId));
        }
        if (request.employeeCode() != null && !request.employeeCode().isBlank()) {
            employee.setEmployeeCode(resolveEmployeeCode(request.employeeCode(), organizationId, employee.getId()));
        }
        assertUniqueContact(request, organizationId, employee.getId(), idOf(employee.getUser()));
        applyRequest(employee, request, organizationId);

        if (!Objects.equals(oldPosition, employee.getPosition())) {
            eventService.record(employee, EmployeeEventType.PROMOTION, "Position changed",
                    oldPosition, employee.getPosition(), null);
        }
        if (!Objects.equals(oldBranchId, idOf(employee.getBranch()))
                || !Objects.equals(oldDepartmentId, idOf(employee.getDepartment()))) {
            eventService.record(employee, EmployeeEventType.TRANSFER, "Branch or department changed",
                    oldBranchId + "/" + oldDepartmentId,
                    idOf(employee.getBranch()) + "/" + idOf(employee.getDepartment()), null);
        }
        return EmployeeMapper.toResponse(employee);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findAccessibleEntity(id);
        employeeRepository.delete(employee);
    }

    private void applyRequest(Employee employee, EmployeeRequest request, Long organizationId) {
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setPosition(request.position());
        employee.setNationalIdSerial(request.nationalIdSerial());
        employee.setPhotoUrl(request.photoUrl());
        employee.setAddress(request.address());
        employee.setEmergencyContactName(request.emergencyContactName());
        employee.setEmergencyContactPhone(request.emergencyContactPhone());
        employee.setEmergencyContactRelation(request.emergencyContactRelation());
        employee.setBranch(resolveBranch(request.branchId(), organizationId));
        employee.setDepartment(resolveDepartment(request.departmentId(), organizationId));
        employee.setManager(resolveManager(request.managerId(), organizationId, employee));
        employee.setEmploymentType(request.employmentType());
        employee.setJoinedAt(request.joinedAt());
        employee.setActive(request.active() == null || request.active());
    }

    /**
     * Email must be unique across all employees and logins (it is the login); phone must be unique
     * within the organization. {@code selfId} / {@code selfUserId} exclude the employee being updated.
     */
    private void assertUniqueContact(EmployeeRequest request, Long organizationId, Long selfId, Long selfUserId) {
        String email = request.email() == null ? null : request.email().trim();
        if (email != null && !email.isEmpty()) {
            boolean employeeTaken = selfId == null
                    ? employeeRepository.existsByEmailIgnoreCase(email)
                    : employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, selfId);
            boolean userTaken = selfUserId == null
                    ? userRepository.existsByEmailIgnoreCase(email)
                    : userRepository.existsByEmailIgnoreCaseAndIdNot(email, selfUserId);
            // A new employee may be linked to an existing login (userId); that login's own email is fine.
            if (userTaken && selfUserId == null && request.userId() != null) {
                userTaken = userRepository.existsByEmailIgnoreCaseAndIdNot(email, request.userId());
            }
            if (employeeTaken || userTaken) {
                throw new DuplicateResourceException("Email '" + email + "' is already used by another employee or user");
            }
        }
        String phone = request.phone() == null ? null : request.phone().trim();
        if (phone != null && !phone.isEmpty()) {
            boolean taken = selfId == null
                    ? employeeRepository.existsByOrganizationIdAndPhone(organizationId, phone)
                    : employeeRepository.existsByOrganizationIdAndPhoneAndIdNot(organizationId, phone, selfId);
            if (taken) {
                throw new DuplicateResourceException("Phone '" + phone + "' is already used by another employee in this organization");
            }
        }
    }

    /** Creates the mobile-app login for a new employee and gives it the EMPLOYEE role. */
    private User createLogin(EmployeeRequest request, Organization organization) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required to create the employee's login (or pass userId)");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required to create the employee's login (or pass userId)");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }
        User user = new User();
        user.setOrganization(organization);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setUserType(UserType.ORG_USER);
        user.setActive(true);
        user.setRoles(new java.util.HashSet<>(Set.of(roleProvisioner.employeeRole(organization))));
        return userRepository.save(user);
    }

    private User resolveExistingUser(Long userId, Long organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        employeeRepository.findByUserId(userId).ifPresent(existing -> {
            throw new DuplicateResourceException("User " + userId + " is already linked to employee " + existing.getId());
        });
        return user;
    }

    private String resolveEmployeeCode(String requested, Long organizationId, Long selfId) {
        if (requested != null && !requested.isBlank()) {
            boolean taken = selfId == null
                    ? employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, requested)
                    : employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCaseAndIdNot(organizationId, requested, selfId);
            if (taken) {
                throw new DuplicateResourceException("Employee code '" + requested + "' is already used");
            }
            return requested;
        }
        long next = employeeRepository.countByOrganizationId(organizationId) + 1;
        String code = String.format("EMP-%04d", next);
        while (employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, code)) {
            code = String.format("EMP-%04d", ++next);
        }
        return code;
    }

    private Branch resolveBranch(Long id, Long organizationId) {
        if (id == null) {
            return null;
        }
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
        if (!branch.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Branch not found: " + id);
        }
        return branch;
    }

    private Department resolveDepartment(Long id, Long organizationId) {
        if (id == null) {
            return null;
        }
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        if (!department.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Department not found: " + id);
        }
        return department;
    }

    private Employee resolveManager(Long id, Long organizationId, Employee self) {
        if (id == null) {
            return null;
        }
        if (self.getId() != null && self.getId().equals(id)) {
            throw new IllegalArgumentException("An employee cannot be their own manager");
        }
        Employee manager = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + id));
        if (!manager.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Manager not found: " + id);
        }
        return manager;
    }

    private static Long idOf(com.mindhr.entity.base.BaseEntity entity) {
        return entity != null ? entity.getId() : null;
    }

    private Employee findAccessibleEntity(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        scope.assertCanAccess(employee);
        return employee;
    }
}
