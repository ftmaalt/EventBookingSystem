package com.bookngo.bookingservice.repository;

import com.bookngo.bookingservice.model.Sessions;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionsRepository extends JpaRepository<Sessions, Long> {
    List<Sessions> findByActivityId(Long activityId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sessions s where s.id = :id")
    Optional<Sessions> findByIdForUpdate(@Param("id") Long id);
}
