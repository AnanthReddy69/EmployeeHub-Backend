package com.anurag.ai.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.ai.entity.Employee;
import com.anurag.ai.repository.EmployeeRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Employee registerEmployee(Employee employee) {

        if (employee.getName() == null || employee.getName().isBlank()
                || employee.getEmail() == null || employee.getEmail().isBlank()
                || employee.getPassword() == null || employee.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "Name, email and password are required");
        }

        if (employeeRepository.findByEmail(employee.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        // Never accept a privileged role from public registration.
        employee.setRole("EMPLOYEE");

        return employeeRepository.save(employee);
    }

    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee updated) {

        Employee employee = getEmployeeById(id);

        if (updated.getName() == null || updated.getName().isBlank()
                || updated.getEmail() == null || updated.getEmail().isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }

        employeeRepository.findByEmail(updated.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already registered");
                });

        employee.setName(updated.getName().trim());
        employee.setEmail(updated.getEmail().trim());
        employee.setDepartment(updated.getDepartment());
        employee.setDesignation(updated.getDesignation());
        employee.setSalary(updated.getSalary());

        if (updated.getPassword() != null
                && !updated.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        // Role is deliberately not changed here.
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {

        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("Employee not found");
        }

        employeeRepository.deleteById(id);
    }

    @Transactional
    public Employee changeRole(Long id, String requestedRole) {

        if (requestedRole == null) {
            throw new IllegalArgumentException("Role is required");
        }

        String role = requestedRole.trim().toUpperCase(Locale.ROOT);

        if (!List.of("EMPLOYEE", "HR", "MANAGER").contains(role)) {
            throw new IllegalArgumentException(
                    "Role must be EMPLOYEE, HR or MANAGER");
        }

        Employee employee = getEmployeeById(id);
        employee.setRole(role);

        return employeeRepository.save(employee);
    }
}