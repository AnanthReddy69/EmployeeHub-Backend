package com.anurag.ai.repository;

import com.anurag.ai.entity.LeaveRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployee_IdOrderByCreatedAtDesc(
        Long employeeId
    );

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}