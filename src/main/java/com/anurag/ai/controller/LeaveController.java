package com.anurag.ai.controller;

import com.anurag.ai.entity.Employee;
import com.anurag.ai.entity.LeaveRequest;
import com.anurag.ai.repository.EmployeeRepository;
import com.anurag.ai.repository.LeaveRequestRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveController(
            LeaveRequestRepository leaveRepository,
            EmployeeRepository employeeRepository) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
    }

    public record CreateLeaveRequest(
        @NotBlank String leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String reason
    ) {}

    public record DecisionRequest(
        @NotBlank String decision,
        String note
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
            @Valid @RequestBody CreateLeaveRequest request) {

        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End date cannot be before start date"
            );
        }

        if (request.startDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Start date cannot be in the past"
            );
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(currentEmployee());
        leave.setLeaveType(request.leaveType().trim());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(request.reason().trim());

        return view(leaveRepository.save(leave));
    }

    @GetMapping("/me")
    public List<Map<String, Object>> mine() {
        return leaveRepository
            .findByEmployee_IdOrderByCreatedAtDesc(currentEmployee().getId())
            .stream()
            .map(this::view)
            .toList();
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        return leaveRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::view)
            .toList();
    }

    @PatchMapping("/{id}/decision")
    public Map<String, Object> decide(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {

        String decision = request.decision()
            .trim().toUpperCase(Locale.ROOT);

        if (!decision.equals("APPROVED")
                && !decision.equals("REJECTED")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Decision must be APPROVED or REJECTED"
            );
        }

        LeaveRequest leave = leaveRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave request not found"
            ));

        if (!"PENDING".equals(leave.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This leave request has already been decided"
            );
        }

        leave.setStatus(decision);
        leave.setDecisionBy(SecurityContextHolder.getContext()
            .getAuthentication().getName());
        leave.setDecisionNote(request.note());
        leave.setDecidedAt(LocalDateTime.now());

        return view(leaveRepository.save(leave));
    }

    private Employee currentEmployee() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        return employeeRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Account not found"
            ));
    }

    private Map<String, Object> view(LeaveRequest leave) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", leave.getId());
        result.put("employeeId", leave.getEmployee().getId());
        result.put("employeeName", leave.getEmployee().getName());
        result.put("employeeEmail", leave.getEmployee().getEmail());
        result.put("leaveType", leave.getLeaveType());
        result.put("startDate", leave.getStartDate());
        result.put("endDate", leave.getEndDate());
        result.put("reason", leave.getReason());
        result.put("status", leave.getStatus());
        result.put("createdAt", leave.getCreatedAt());
        result.put("decisionBy", leave.getDecisionBy());
        result.put("decisionNote", leave.getDecisionNote());
        return result;
    }
}