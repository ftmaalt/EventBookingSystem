package com.project.bookngo.repository;

import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.WaitListStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.project.bookngo.model.response.WaitlistedSessionResponse;

import java.util.List;
import java.util.Optional;

public interface WaitingListRepository   extends JpaRepository<WaitlistEntry, Long> {
    @Query(value = """
            SELECT new com.project.bookngo.model.response.WaitlistedSessionResponse(
                e.entryId, s.id, a.id, a.title, s.startTime, s.endTime, e.joinedAt,
                (SELECT COUNT(ahead) FROM WaitlistEntry ahead
                 WHERE ahead.session.id = s.id
                   AND ahead.status = com.project.bookngo.model.enums.WaitListStatus.WAITING
                   AND ahead.entryId < e.entryId) + 1)
            FROM WaitlistEntry e JOIN e.session s JOIN s.activity a
            WHERE e.user.id = :userId
              AND e.status = com.project.bookngo.model.enums.WaitListStatus.WAITING
            """, countQuery = """
            SELECT COUNT(e) FROM WaitlistEntry e
            WHERE e.user.id = :userId
              AND e.status = com.project.bookngo.model.enums.WaitListStatus.WAITING
            """)
    Page<WaitlistedSessionResponse> findWaitingSessionsByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    Optional<WaitlistEntry> findByUserIdAndSessionIdAndStatus(Long userId, Long sessionId, WaitListStatus status);
    List<WaitlistEntry> findBySessionIdAndStatusOrderByEntryIdAsc(Long sessionId, WaitListStatus status);
    long countBySessionIdAndStatusAndEntryIdLessThan(Long sessionId, WaitListStatus status, Long entryId);

    boolean existsBySessionIdAndStatus(Long id, WaitListStatus waitListStatus);
}
