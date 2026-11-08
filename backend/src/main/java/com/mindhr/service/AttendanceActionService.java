package com.mindhr.service;

import com.mindhr.dto.AttendanceResponse;
import com.mindhr.dto.CheckInRequest;
import com.mindhr.entity.Attendance;
import com.mindhr.entity.Branch;
import com.mindhr.entity.Employee;
import com.mindhr.entity.Shift;
import com.mindhr.entity.ShiftAssignment;
import com.mindhr.entity.enums.AttendanceMethod;
import com.mindhr.entity.enums.AttendanceStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.AttendanceMapper;
import com.mindhr.repository.AttendanceRepository;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.ShiftAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Mobile check-in / check-out for the signed-in employee. Times use the server clock and the
 * server's time zone. Lateness is measured against the shift on the employee's roster for the
 * day (no grace period); with no roster entry the employee is simply PRESENT.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceActionService {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final DataScopeService scope;

    public AttendanceResponse checkIn(CheckInRequest request) {
        Employee employee = currentEmployee();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        AttendanceMethod method = request.method() != null ? request.method() : AttendanceMethod.MOBILE_GPS;
        verifyLocation(employee, method, request.latitude(), request.longitude());

        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseGet(() -> {
                    Attendance created = new Attendance();
                    created.setOrganization(employee.getOrganization());
                    created.setEmployee(employee);
                    created.setWorkDate(today);
                    return created;
                });
        if (attendance.getStatus() == AttendanceStatus.ON_LEAVE && attendance.getCheckInAt() == null) {
            throw new IllegalArgumentException("You are on approved leave today");
        }
        if (attendance.getCheckInAt() != null) {
            throw new IllegalArgumentException("Already checked in today");
        }

        attendance.setCheckInAt(now);
        attendance.setCheckInLatitude(request.latitude());
        attendance.setCheckInLongitude(request.longitude());
        attendance.setMethod(method);
        attendance.setNote(request.note());

        int lateMinutes = 0;
        Optional<Shift> shift = shiftAssignmentRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .map(ShiftAssignment::getShift);
        if (shift.isPresent()) {
            LocalDateTime scheduledStart = today.atTime(shift.get().getStartTime());
            if (now.isAfter(scheduledStart)) {
                lateMinutes = (int) Duration.between(scheduledStart, now).toMinutes();
            }
        }
        attendance.setLateMinutes(lateMinutes);
        attendance.setStatus(lateMinutes > 0 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        return AttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    public AttendanceResponse checkOut(CheckInRequest request) {
        Employee employee = currentEmployee();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = openRecord(employee, today)
                .or(() -> openRecord(employee, today.minusDays(1)))
                .orElseThrow(() -> new IllegalArgumentException("You have not checked in"));

        AttendanceMethod method = request.method() != null ? request.method() : AttendanceMethod.MOBILE_GPS;
        verifyLocation(employee, method, request.latitude(), request.longitude());

        attendance.setCheckOutAt(now);
        attendance.setCheckOutLatitude(request.latitude());
        attendance.setCheckOutLongitude(request.longitude());
        attendance.setWorkedMinutes((int) Duration.between(attendance.getCheckInAt(), now).toMinutes());
        return AttendanceMapper.toResponse(attendance);
    }

    /** Today's record for the signed-in employee, if any. */
    @Transactional(readOnly = true)
    public Optional<AttendanceResponse> today() {
        Long employeeId = scope.requireCurrentEmployeeId();
        return attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now())
                .map(AttendanceMapper::toResponse);
    }

    private Optional<Attendance> openRecord(Employee employee, LocalDate date) {
        return attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), date)
                .filter(a -> a.getCheckInAt() != null && a.getCheckOutAt() == null);
    }

    private Employee currentEmployee() {
        Long id = scope.requireCurrentEmployeeId();
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
    }

    /** GPS/selfie check-ins must be inside the branch geofence when the branch has one configured. */
    private void verifyLocation(Employee employee, AttendanceMethod method, Double latitude, Double longitude) {
        Branch branch = employee.getBranch();
        if (branch == null || branch.getGeofenceRadiusM() == null
                || branch.getLatitude() == null || branch.getLongitude() == null) {
            return;
        }
        if (method != AttendanceMethod.MOBILE_GPS && method != AttendanceMethod.SELFIE) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Location is required to check in at this branch");
        }
        double distance = distanceMetres(latitude, longitude, branch.getLatitude(), branch.getLongitude());
        if (distance > branch.getGeofenceRadiusM()) {
            throw new IllegalArgumentException("You are outside the allowed area for " + branch.getName()
                    + " (" + Math.round(distance) + " m away, limit " + branch.getGeofenceRadiusM() + " m)");
        }
    }

    /** Haversine great-circle distance. */
    static double distanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
