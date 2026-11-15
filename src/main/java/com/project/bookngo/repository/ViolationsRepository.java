package com.project.bookngo.repository;

import com.bookngo.notificationservice.event.model.Violations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationsRepository extends JpaRepository<Violations, Long> {
    List<Violations> findByUserIdOrderByCreatedAtDesc(Long userId);
}
