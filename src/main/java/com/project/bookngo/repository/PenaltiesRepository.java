package com.project.bookngo.repository;

import com.bookngo.notificationservice.event.model.Penalties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltiesRepository extends JpaRepository<Penalties, Long> {
    List<Penalties> findByUserId(Long userId);
}