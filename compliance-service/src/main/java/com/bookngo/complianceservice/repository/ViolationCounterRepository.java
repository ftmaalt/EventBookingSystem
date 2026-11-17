package com.bookngo.complianceservice.repository;

import com.bookngo.complianceservice.model.ViolationCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ViolationCounterRepository extends JpaRepository<ViolationCounter, Long> {

    // Row lock: two violations for the same user are counted one after the other, never both on the same old value.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ViolationCounter c where c.userId = :userId")
    Optional<ViolationCounter> findByUserIdForUpdate(@Param("userId") Long userId);
}
