package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.SessionStatus;
import com.project.bookngo.model.enums.WaitListStatus;
import com.project.bookngo.model.response.WaitListResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.WaitingListRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingService {
    private final WaitingListRepository waitlistRepository;
    private final SessionsRepository sessionsRepository;
    private final BookingsRepository bookingsRepository;
    private final CurrentUserService currentUserService;


    @Transactional
    public WaitListResponse joinWaitlist(Long sessionId) {
        User user = currentUserService.getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(sessionId).orElseThrow(() -> new EntityNotFoundException("No session with id : " + sessionId));

        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED){
            throw new BusinessConflictsException("The waitlist is unavailable for this session.");
        }

        if (!session.getStartTime().isAfter(LocalDateTime.now())){
            throw new BusinessConflictsException("You cannot join the waitlist for a session that has already started");
        }

        if (session.getSpotsLeft() > 0) {
            throw new BusinessConflictsException("This session still has available spots. You can book directly.");
        }
        boolean hasBooking =
                bookingsRepository.existsByUserIdAndSessionIdAndStatusIn(user.getId(), sessionId, List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));

        if (hasBooking) {
            throw new BusinessConflictsException("You already have an active booking for this session.");
        }

        boolean alreadyWaiting = waitlistRepository.existsByUserIdAndSessionIdAndStatus(user.getId(), sessionId, WaitListStatus.WAITING);
        if (alreadyWaiting){
            throw new BusinessConflictsException( "You are already on the waitlist for this session.");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setSession(session);
        entry.setStatus(WaitListStatus.WAITING);

        WaitlistEntry savedEntry = waitlistRepository.save(entry);

        long position = waitlistRepository.countBySessionIdAndStatusAndIdLessThan(sessionId, WaitListStatus.WAITING, savedEntry.getEntryId())+ 1;

        return new WaitListResponse(savedEntry.getEntryId(), sessionId, savedEntry.getStatus(), savedEntry.getJoinedAt(), position);
    }

}
