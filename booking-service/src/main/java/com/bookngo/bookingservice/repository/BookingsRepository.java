package com.project.bookngo.repository;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    List<Bookings> findByUserId(Long userId);
    List<Bookings> findBySessionId(Long sessionId);
    @Query("""
    SELECT b FROM Bookings b
    JOIN FETCH b.session s
    JOIN FETCH b.user u
    WHERE b.status = :status
      AND s.startTime BETWEEN :start AND :end
    """)
    List<Bookings> findUpcomingBookingsForReminders( @Param("status") BookingStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Optional<Bookings> findByBookingIdAndUserId(Long bookingId, Long userId);
    Optional<Bookings> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, BookingStatus status);
    boolean existsByUserIdAndSessionIdAndStatusIn(Long userId, Long sessionId, Collection<BookingStatus> statuses);
    @Query("SELECT b FROM Bookings b WHERE b.user.id = :userId AND (:status IS NULL OR b.status = :status)")
    Page<Bookings> findMyBookings(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "session", "session.activity"})
    @Query("SELECT b FROM Bookings b WHERE b.bookingId = :bookingId")
    Optional<Bookings> findForNotification(@Param("bookingId") Long bookingId);

}