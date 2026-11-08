package com.mindhr.controller;

import com.mindhr.dto.AttendanceResponse;
import com.mindhr.dto.CheckInRequest;
import com.mindhr.service.AttendanceActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Mobile app check-in / check-out for the signed-in employee. */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceActionController {

    private final AttendanceActionService service;

    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(service.checkIn(request));
    }

    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(service.checkOut(request));
    }

    /** Today's record, or 204 if the employee has not checked in yet. */
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> today() {
        return service.today().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
