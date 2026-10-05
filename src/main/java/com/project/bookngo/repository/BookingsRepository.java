package com.project.bookngo.repository;

import com.project.bookngo.model.Bookings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    List<Bookings> findByUserId(Long userId);
    List<Bookings> findBySessionId(Long sessionId);
    Optional<Bookings> findByIdAndUserId(Long id, Long userId);
}