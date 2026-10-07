package com.project.bookngo.repository;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    List<Bookings> findByUserId(Long userId);
    List<Bookings> findBySessionId(Long sessionId);
    List<Bookings> findByStatusAndSession_StartTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);
    Optional<Bookings> findByIdAndUserId(Long id, Long userId);
    Optional<Bookings> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, BookingStatus status);
}