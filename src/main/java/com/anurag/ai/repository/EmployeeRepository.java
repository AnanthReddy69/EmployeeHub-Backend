package com.anurag.ai.repository;

import com.anurag.ai.entity.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
}