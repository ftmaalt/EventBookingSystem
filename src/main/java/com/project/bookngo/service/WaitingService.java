package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.response.WaitListResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.WaitingListRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
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
        Sessions session = sessionsRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new InformationNotFoundException("No session with ID: " + sessionId));

        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED){
            throw new BusinessConflictsException("The waitlist is unavailable for this session.");
        }

        if (!session.getStartTime().isAfter(LocalDateTime.now())){
            throw new BusinessConflictsException("You cannot join the waitlist for a session that has already started");
        }

        if (session.getSpotsLeft() > 0) {
            throw new BusinessConflictsException("This session still has available spots. You can book directly.");
        }
        if (user.getRole() != UserRole.USER || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Only active customers can join the waitlist.");
        }
        boolean hasBooking = bookingsRepository.existsByUserIdAndSessionIdAndStatusIn(user.getId(), sessionId, List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));

        if (hasBooking) {
            throw new BusinessConflictsException("You already have an active booking for this session.");
        }

        if (session.getActivity().getStatus() != ActivityStatus.ACTIVE) {
            throw new BusinessConflictsException("This activity is currently unavailable.");
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

        long position = waitlistRepository.countBySessionIdAndStatusAndEntryIdLessThan(sessionId, WaitListStatus.WAITING, savedEntry.getEntryId())+ 1;

        return new WaitListResponse(savedEntry.getEntryId(), sessionId, savedEntry.getStatus(), savedEntry.getJoinedAt(), position);
    }

    @Transactional
    public String leaveWaitlist(Long sessionId) {
        User user = currentUserService.getCurrentUser();

        sessionsRepository.findByIdForUpdate(sessionId).orElseThrow(() -> new InformationNotFoundException("Session with Id " + sessionId +" doesn't exist."));

        WaitlistEntry entry = waitlistRepository.findByUserIdAndSessionIdAndStatus(user.getId(), sessionId, WaitListStatus.WAITING).orElseThrow(() -> new InformationNotFoundException("You do not have an active waitlist entry for this session."));

        entry.setStatus(WaitListStatus.CANCELLED);
        waitlistRepository.save(entry);

        return "You have successfully left the waitlist.";
    }

    @Transactional(readOnly = true)
    public WaitListResponse getMyWaitListEntry(Long sessionId){
        User user= currentUserService.getCurrentUser();

        WaitlistEntry entry = waitlistRepository.findByUserIdAndSessionIdAndStatus(user.getId(), sessionId, WaitListStatus.WAITING).orElseThrow(() -> new InformationNotFoundException("You did not apply for the waitlist for this session"));

        long position = waitlistRepository.countBySessionIdAndStatusAndEntryIdLessThan(sessionId, WaitListStatus.WAITING, entry.getEntryId())+1;

        return new WaitListResponse(entry.getEntryId(), sessionId, entry.getStatus(), entry.getJoinedAt(), position);
    }



}
