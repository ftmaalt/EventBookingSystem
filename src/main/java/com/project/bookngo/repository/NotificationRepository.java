package com.project.bookngo.repository;

import com.project.bookngo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    List<Notification> findByUserIdAndReadFalse(Long userId);
    void deleteByUserId(Long userId);
}
