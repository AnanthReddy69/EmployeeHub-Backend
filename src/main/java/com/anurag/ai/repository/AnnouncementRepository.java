package com.anurag.ai.repository;

import com.anurag.ai.entity.Announcement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByCreatedAtDesc();
}