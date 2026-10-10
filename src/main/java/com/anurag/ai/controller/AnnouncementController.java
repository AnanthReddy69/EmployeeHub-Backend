package com.anurag.ai.controller;

import com.anurag.ai.entity.Announcement;
import com.anurag.ai.repository.AnnouncementRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repository;

    public AnnouncementController(AnnouncementRepository repository) {
        this.repository = repository;
    }

    public record AnnouncementRequest(
        @NotBlank String title,
        @NotBlank String message
    ) {}

    @GetMapping
    public List<Announcement> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public Announcement create(
            @Valid @RequestBody AnnouncementRequest request) {

        Announcement announcement = new Announcement();
        announcement.setTitle(request.title().trim());
        announcement.setMessage(request.message().trim());
        announcement.setCreatedBy(
            SecurityContextHolder.getContext()
                .getAuthentication().getName()
        );

        return repository.save(announcement);
    }
}