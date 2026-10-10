package com.anurag.ai.controller;

import com.anurag.ai.entity.Employee;
import com.anurag.ai.repository.EmployeeRepository;
import com.anurag.ai.service.JWTService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public EmployeeController(
            EmployeeRepository repository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JWTService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String password,
        String department,
        String designation
    ) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record EmployeeUpdate(
        String name,
        String email,
        String department,
        String designation,
        BigDecimal salary,
        String password
    ) {}

    public record RoleRequest(@NotBlank String role) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            @Valid @RequestBody RegisterRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered"
            );
        }

        Employee employee = new Employee();
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setDepartment(request.department());
        employee.setDesignation(request.designation());

        // Public registration must never grant HR or manager privileges.
        employee.setRole("EMPLOYEE");

        return safeEmployee(repository.save(employee), true);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication =
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email, request.password()
                )
            );

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        Employee employee = repository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid credentials"
            ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Login successful");
        result.put("token", jwtService.generateToken(employee.getEmail()));
        result.put("name", employee.getName());
        result.put("email", employee.getEmail());
        result.put("role", employee.getRole());

        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Employee employee = currentEmployee();
        return safeEmployee(employee, true);
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        boolean salaryAllowed = hasRole("HR") || hasRole("MANAGER");

        return repository.findAll()
            .stream()
            .map(employee -> safeEmployee(employee, salaryAllowed))
            .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable Long id) {
        Employee employee = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));

        return safeEmployee(
            employee, hasRole("HR") || hasRole("MANAGER")
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody EmployeeUpdate request) {

        Employee employee = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));

        if (request.name() != null && !request.name().isBlank()) {
            employee.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);

            if (!email.equalsIgnoreCase(employee.getEmail())
                    && repository.existsByEmail(email)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already in use"
                );
            }

            employee.setEmail(email);
        }

        if (request.department() != null) {
            employee.setDepartment(request.department());
        }

        if (request.designation() != null) {
            employee.setDesignation(request.designation());
        }

        if (hasRole("HR") || hasRole("MANAGER")) {
            if (request.salary() != null) {
                if (request.salary().signum() < 0) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Salary cannot be negative"
                    );
                }
                employee.setSalary(request.salary());
            }

            if (request.password() != null
                    && !request.password().isBlank()) {
                employee.setPassword(
                    passwordEncoder.encode(request.password())
                );
            }
        }

        return safeEmployee(
            repository.save(employee),
            hasRole("HR") || hasRole("MANAGER")
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            );
        }

        if (currentEmployee().getId().equals(id) && hasRole("MANAGER")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot delete your own manager account"
            );
        }

        repository.deleteById(id);
    }

    @PatchMapping("/{id}/role")
    public Map<String, Object> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {

        String role = request.role().trim().toUpperCase(Locale.ROOT);

        if (!List.of("EMPLOYEE", "HR", "MANAGER").contains(role)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Role must be EMPLOYEE, HR or MANAGER"
            );
        }

        Employee employee = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));

        employee.setRole(role);

        return safeEmployee(repository.save(employee), true);
    }

    private Employee currentEmployee() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        return repository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Account not found"
            ));
    }

    private boolean hasRole(String role) {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
            && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_" + role)
                );
    }

    private Map<String, Object> safeEmployee(
            Employee employee, boolean includeSalary) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", employee.getId());
        result.put("name", employee.getName());
        result.put("email", employee.getEmail());
        result.put("department", employee.getDepartment());
        result.put("designation", employee.getDesignation());
        result.put("role", employee.getRole());

        if (includeSalary) {
            result.put("salary", employee.getSalary());
        }

        // Never return the password or password hash.
        return result;
    }
}