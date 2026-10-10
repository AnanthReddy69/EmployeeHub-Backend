package com.anurag.ai.service;

import com.anurag.ai.entity.Employee;
import com.anurag.ai.repository.EmployeeRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public MyUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Employee employee = employeeRepository.findByEmail(email)
            .orElseThrow(() ->
                new UsernameNotFoundException("Account not found")
            );

        String role = employee.getRole() == null
            ? "EMPLOYEE"
            : employee.getRole().toUpperCase(Locale.ROOT);

        if (!role.equals("EMPLOYEE")
                && !role.equals("HR")
                && !role.equals("MANAGER")) {
            throw new UsernameNotFoundException("Invalid account role");
        }

        return User.withUsername(employee.getEmail())
            .password(employee.getPassword())
            .roles(role)
            .build();
    }
}