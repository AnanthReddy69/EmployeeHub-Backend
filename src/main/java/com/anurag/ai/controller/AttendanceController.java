package com.anurag.ai.controller;

import com.anurag.ai.entity.Attendance;
import com.anurag.ai.entity.Employee;
import com.anurag.ai.repository.AttendanceRepository;
import com.anurag.ai.repository.EmployeeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/check-in")
    public Map<String, Object> checkIn() {
        Employee employee = currentEmployee();
        LocalDate today = LocalDate.now();

        if (attendanceRepository
                .findByEmployee_IdAndWorkDate(employee.getId(), today)
                .isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "You have already checked in today"
            );
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setWorkDate(today);
        attendance.setCheckIn(LocalDateTime.now());
        attendance.setStatus("PRESENT");

        return view(attendanceRepository.save(attendance));
    }

    @PostMapping("/check-out")
    public Map<String, Object> checkOut() {
        Employee employee = currentEmployee();

        Attendance attendance = attendanceRepository
            .findByEmployee_IdAndWorkDate(
                employee.getId(), LocalDate.now()
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Check in before checking out"
            ));

        if (attendance.getCheckOut() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "You have already checked out"
            );
        }

        attendance.setCheckOut(LocalDateTime.now());

        return view(attendanceRepository.save(attendance));
    }

    @GetMapping("/me")
    public List<Map<String, Object>> mine() {
        Employee employee = currentEmployee();

        return attendanceRepository
            .findByEmployee_IdOrderByWorkDateDesc(employee.getId())
            .stream()
            .map(this::view)
            .toList();
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        return attendanceRepository.findAllByOrderByWorkDateDesc()
            .stream()
            .map(this::view)
            .toList();
    }

    private Employee currentEmployee() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        return employeeRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Account not found"
            ));
    }

    private Map<String, Object> view(Attendance attendance) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", attendance.getId());
        result.put("employeeId", attendance.getEmployee().getId());
        result.put("employeeName", attendance.getEmployee().getName());
        result.put("email", attendance.getEmployee().getEmail());
        result.put("date", attendance.getWorkDate());
        result.put("checkIn", attendance.getCheckIn());
        result.put("checkOut", attendance.getCheckOut());
        result.put("status", attendance.getStatus());
        return result;
    }
}