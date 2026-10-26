package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.response.WaitListResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.WaitingListRepository;
import com.project.bookngo.service.event.BookingNotificationEvent;
import com.project.bookngo.service.event.BookingNotificationType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingService {

    private final WaitingListRepository waitlistRepository;
    private final SessionsRepository sessionsRepository;
    private final BookingsRepository bookingsRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;


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

    /**
     * Allocates available seats to waiting customers in FIFO order.
     *
     * Must be called within an existing transaction after acquiring
     * a PESSIMISTIC_WRITE lock on the session row.
     *
     * Booking creation, capacity updates, and waitlist status changes
     * must commit or roll back together.
     */

    @Transactional(propagation = Propagation.MANDATORY)
    public void allocateNextWaitlistedUser(Sessions session) {
        if (session.getSpotsLeft() <= 0)
            return;

        if (!session.getStartTime().isAfter(LocalDateTime.now()) || session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED ||
                session.getActivity().getStatus() != ActivityStatus.ACTIVE) {
            return;
        }

        List<WaitlistEntry> waitingEntries = waitlistRepository.findBySessionIdAndStatusOrderByEntryIdAsc(session.getId(), WaitListStatus.WAITING);
        for (WaitlistEntry entry : waitingEntries ) {
            if (session.getSpotsLeft() <= 0)
                break;

            User user = entry.getUser();
            if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getRole() != UserRole.USER)
                continue;

            boolean alreadyBooked = bookingsRepository.existsByUserIdAndSessionIdAndStatusIn(user.getId(), session.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));

            if (alreadyBooked)
                continue;

            Bookings newBooking = new Bookings();

            newBooking.setUser(user);
            newBooking.setSession(session);
            newBooking.setParticipants(1);
            newBooking.setStatus(BookingStatus.CONFIRMED);
            newBooking.setBookingType(BookingType.INDIVIDUAL);
            newBooking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

            BigDecimal subtotal = session.getActivity().getPricePerPerson();

            newBooking.setSubtotal(subtotal);
            newBooking.setDiscountPercent(BigDecimal.ZERO);
            newBooking.setTotalPrice(subtotal);

            newBooking.setCreatedBy("SYSTEM_WAITLIST");
            newBooking.setUpdatedBy("SYSTEM_WAITLIST");

           Bookings saved =  bookingsRepository.save(newBooking);
            eventPublisher.publishEvent(new BookingNotificationEvent(saved.getBookingId(), BookingNotificationType.WAITLIST_ALLOCATED));

            session.setSpotsLeft(session.getSpotsLeft()-1);
            if (session.getSpotsLeft() == 0) {
                session.setStatus(SessionStatus.FULL);
            }

            entry.setStatus(WaitListStatus.BOOKED);
            waitlistRepository.save(entry);
        }


    }





}
