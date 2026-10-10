package com.anurag.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_employee_date",
        columnNames = {"employee_id", "work_date"}
    )
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    @Column(nullable = false, length = 20)
    private String status = "PRESENT";

    public Long getId() { return id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalDateTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDateTime checkIn) { this.checkIn = checkIn; }

    public LocalDateTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDateTime checkOut) { this.checkOut = checkOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}