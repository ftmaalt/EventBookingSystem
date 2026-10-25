package com.project.bookngo.repository;

import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.WaitListStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitingListRepository   extends JpaRepository<WaitlistEntry, Long> {
    boolean existsByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    Optional<WaitlistEntry> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    List<WaitlistEntry> findBySessionIdAndStatusOrderByEntryIdAsc(Long sessionId, WaitListStatus status);
    long countBySessionIdAndStatusAndEntryIdLessThan(Long sessionId, WaitListStatus status, Long entryId);
}
