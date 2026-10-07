package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    List<Bookings> findByUserId(Long userId);
    List<Bookings> findBySessionId(Long sessionId);
    List<Bookings> findByStatusAndSession_StartTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);
    Optional<Bookings> findByBookingIdAndUserId(Long bookingId, Long userId);
    Optional<Bookings> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, BookingStatus status);
    @Query("SELECT b FROM BOOKINGS b WHERE b.user.id = :userId AND (:status IS NULL OR b.status = :status)")
    Page<Bookings> findMyBookings(@Param("userid") Long userId, @Param("status") BookingStatus status, Pageable pageable);
}