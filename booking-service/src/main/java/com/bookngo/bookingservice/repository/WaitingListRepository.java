package com.bookngo.bookingservice.repository;

import com.bookngo.bookingservice.dto.response.WaitlistedSessionResponse;
import com.bookngo.bookingservice.model.WaitlistEntry;
import com.bookngo.bookingservice.model.enums.WaitListStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitingListRepository extends JpaRepository<WaitlistEntry, Long> {
    @Query(value = """
            SELECT new com.bookngo.bookingservice.dto.response.WaitlistedSessionResponse(
                e.entryId, s.id, s.activityId, s.activityTitle, s.startTime, s.endTime, e.joinedAt,
                (SELECT COUNT(ahead) FROM WaitlistEntry ahead
                 WHERE ahead.session.id = s.id
                   AND ahead.status = com.bookngo.bookingservice.model.enums.WaitListStatus.WAITING
                   AND ahead.entryId < e.entryId) + 1)
            FROM WaitlistEntry e JOIN e.session s
            WHERE e.userId = :userId
              AND e.status = com.bookngo.bookingservice.model.enums.WaitListStatus.WAITING
            """, countQuery = """
            SELECT COUNT(e) FROM WaitlistEntry e
            WHERE e.userId = :userId
              AND e.status = com.bookngo.bookingservice.model.enums.WaitListStatus.WAITING
            """)
    Page<WaitlistedSessionResponse> findWaitingSessionsByUserId(@Param("userId") Long userId, Pageable pageable);
    boolean existsByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    Optional<WaitlistEntry> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    List<WaitlistEntry> findBySessionIdAndStatusOrderByEntryIdAsc(Long sessionId, WaitListStatus status);
    long countBySessionIdAndStatusAndEntryIdLessThan(Long sessionId, WaitListStatus status, Long entryId);
    boolean existsBySessionIdAndStatus(Long sessionId, WaitListStatus status);
}
