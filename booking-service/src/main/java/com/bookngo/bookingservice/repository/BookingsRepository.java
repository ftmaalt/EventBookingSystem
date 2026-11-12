package com.bookngo.bookingservice.repository;

import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {

    List<Bookings> findBySessionId(Long sessionId);
    Optional<Bookings> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, BookingStatus status);
    boolean existsByUserIdAndSessionIdAndStatusIn(Long userId, Long sessionId, Collection<BookingStatus> statuses);
    @Query("SELECT b FROM Bookings b WHERE b.userId = :userId AND (:status IS NULL OR b.status = :status)")
    Page<Bookings> findMyBookings(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);
}