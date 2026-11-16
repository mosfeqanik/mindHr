package com.mindhr.controller;

import com.mindhr.dto.EmployeeRequest;
import com.mindhr.dto.EmployeeResponse;
import com.mindhr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(employeeService.list(organizationId));
    }

    /** The signed-in employee's own profile. */
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me() {
        return ResponseEntity.ok(employeeService.me());
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(201).body(employeeService.create(request));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
