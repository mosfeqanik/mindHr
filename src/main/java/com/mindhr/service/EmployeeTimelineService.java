package com.mindhr.service;

import com.mindhr.entity.Employee;
import com.mindhr.entity.EmployeeEvent;
import com.mindhr.entity.enums.EmployeeEventType;
import com.mindhr.repository.EmployeeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Writes entries on an employee's timeline; called by the services that change employee state. */
@Service
@RequiredArgsConstructor
public class EmployeeTimelineService {

    private final EmployeeEventRepository eventRepository;

    @Transactional
    public EmployeeEvent record(Employee employee, EmployeeEventType type, String title,
                                String oldValue, String newValue, String details) {
        return record(employee, type, LocalDate.now(), title, oldValue, newValue, details);
    }

    @Transactional
    public EmployeeEvent record(Employee employee, EmployeeEventType type, LocalDate date, String title,
                                String oldValue, String newValue, String details) {
        EmployeeEvent event = new EmployeeEvent();
        event.setOrganization(employee.getOrganization());
        event.setEmployee(employee);
        event.setType(type);
        event.setEventDate(date);
        event.setTitle(title);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setDetails(details);
        return eventRepository.save(event);
    }
}
