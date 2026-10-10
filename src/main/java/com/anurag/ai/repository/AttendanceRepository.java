package com.anurag.ai.repository;

import com.anurag.ai.entity.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployee_IdAndWorkDate(
        Long employeeId, LocalDate workDate
    );

    List<Attendance> findByEmployee_IdOrderByWorkDateDesc(
        Long employeeId
    );

    List<Attendance> findAllByOrderByWorkDateDesc();
}