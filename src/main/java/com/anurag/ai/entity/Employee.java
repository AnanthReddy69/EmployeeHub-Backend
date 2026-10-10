package com.anurag.ai.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String department;
    private String designation;

    @Column(precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false, length = 20)
    private String role = "EMPLOYEE";

    public Employee() {}

    @PrePersist
    public void beforeInsert() {
        if (role == null || role.isBlank()) {
            role = "EMPLOYEE";
        }
        role = role.trim().toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}