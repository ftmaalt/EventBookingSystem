package com.bookngo.bookingservice.service;

import com.bookngo.bookingservice.dto.response.WaitListResponse;
import com.bookngo.bookingservice.dto.response.WaitlistedSessionResponse;
import com.bookngo.bookingservice.event.BookingNotificationEvent;
import com.bookngo.bookingservice.event.BookingNotificationType;
import com.bookngo.bookingservice.exception.BusinessConflictsException;
import com.bookngo.bookingservice.exception.InformationNotFoundException;
import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.Sessions;
import com.bookngo.bookingservice.model.WaitlistEntry;
import com.bookngo.bookingservice.model.enums.*;
import com.bookngo.bookingservice.repository.BookingsRepository;
import com.bookngo.bookingservice.repository.SessionsRepository;
import com.bookngo.bookingservice.repository.WaitingListRepository;
import com.bookngo.bookingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingService {

    private static final Logger logger = LoggerFactory.getLogger(WaitingService.class);

    private final WaitingListRepository waitlistRepository;
    private final SessionsRepository sessionsRepository;
    private final BookingsRepository bookingsRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<WaitlistedSessionResponse> getMyWaitlistedSessions(Pageable pageable) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        return waitlistRepository.findWaitingSessionsByUserId(user.id(), pageable);
    }

    @Transactional
    public WaitListResponse joinWaitlist(Long sessionId) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(sessionId).orElseThrow(() ->
                new InformationNotFoundException("No session with ID: " + sessionId));

        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessConflictsException("The waitlist is unavailable for this session.");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessConflictsException("You cannot join the waitlist for a session that has already started.");
        }
        if (session.getSpotsLeft() > 0) {
            throw new BusinessConflictsException("This session still has available spots. You can book directly.");
        }
        if (!"USER".equals(user.role())) {
            throw new AccessDeniedException("Only customers can join the waitlist.");
        }
        if (bookingsRepository.existsByUserIdAndSessionIdAndStatusIn(user.id(), sessionId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT))) {
            throw new BusinessConflictsException("You already have an active booking for this session.");
        }
        if (waitlistRepository.existsByUserIdAndSessionIdAndStatus(user.id(), sessionId, WaitListStatus.WAITING)) {
            throw new BusinessConflictsException("You are already on the waitlist for this session.");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(user.id());
        entry.setUserEmail(user.email());
        entry.setSession(session);
        entry.setStatus(WaitListStatus.WAITING);
        WaitlistEntry saved = waitlistRepository.save(entry);

        long position = waitlistRepository.countBySessionIdAndStatusAndEntryIdLessThan(
                sessionId, WaitListStatus.WAITING, saved.getEntryId()) + 1;
        return new WaitListResponse(saved.getEntryId(), sessionId, saved.getStatus(), saved.getJoinedAt(), position);
    }

    @Transactional
    public String leaveWaitlist(Long sessionId) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        sessionsRepository.findByIdForUpdate(sessionId).orElseThrow(() ->
                new InformationNotFoundException("Session with Id " + sessionId + " doesn't exist."));
        WaitlistEntry entry = waitlistRepository
                .findByUserIdAndSessionIdAndStatus(user.id(), sessionId, WaitListStatus.WAITING)
                .orElseThrow(() -> new InformationNotFoundException("You are not on the waitlist for this session."));
        entry.setStatus(WaitListStatus.CANCELLED);
        waitlistRepository.save(entry);
        return "You have successfully left the waitlist.";
    }

    @Transactional(readOnly = true)
    public WaitListResponse getMyWaitListEntry(Long sessionId) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        WaitlistEntry entry = waitlistRepository
                .findByUserIdAndSessionIdAndStatus(user.id(), sessionId, WaitListStatus.WAITING)
                .orElseThrow(() -> new InformationNotFoundException("You are not on the waitlist for this session."));
        long position = waitlistRepository.countBySessionIdAndStatusAndEntryIdLessThan(
                sessionId, WaitListStatus.WAITING, entry.getEntryId()) + 1;
        return new WaitListResponse(entry.getEntryId(), sessionId, entry.getStatus(), entry.getJoinedAt(), position);
    }

    /** Called from cancelBooking while the session row is locked, inside the same transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void allocateNextWaitlistedUser(Sessions session) {
        if (session.getSpotsLeft() <= 0) {
            return;
        }
        List<WaitlistEntry> waiting = waitlistRepository
                .findBySessionIdAndStatusOrderByEntryIdAsc(session.getId(), WaitListStatus.WAITING);
        if (waiting.isEmpty()) {
            return;
        }

        boolean sessionUnavailable = !session.getStartTime().isAfter(LocalDateTime.now())
                || session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.IN_PROGRESS;
        if (sessionUnavailable) {
            logger.warn("Expiring {} waitlist entries because session {} is no longer eligible.", waiting.size(), session.getId());
            waiting.forEach(e -> e.setStatus(WaitListStatus.EXPIRED));
            waitlistRepository.saveAll(waiting);
            return;
        }

        for (WaitlistEntry entry : waiting) {
            if (session.getSpotsLeft() <= 0) {
                break;
            }
            if (bookingsRepository.existsByUserIdAndSessionIdAndStatusIn(entry.getUserId(), session.getId(),
                    List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT))) {
                logger.warn("Expiring waitlist entry {}: user {} already has an active booking.", entry.getEntryId(), entry.getUserId());
                entry.setStatus(WaitListStatus.EXPIRED);
                waitlistRepository.save(entry);
                continue;
            }

            BigDecimal subtotal = session.getPricePerPerson();
            Bookings booking = new Bookings();
            booking.setUserId(entry.getUserId());
            booking.setUserEmail(entry.getUserEmail());
            booking.setSession(session);
            booking.setParticipants(1);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setBookingType(BookingType.INDIVIDUAL);
            booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
            booking.setSubtotal(subtotal);
            booking.setDiscountPercent(BigDecimal.ZERO);
            booking.setTotalPrice(subtotal);
            booking.setCreatedBy("SYSTEM_WAITLIST");
            booking.setUpdatedBy("SYSTEM_WAITLIST");
            Bookings saved = bookingsRepository.save(booking);

            session.setSpotsLeft(session.getSpotsLeft() - 1);
            if (session.getSpotsLeft() == 0) {
                session.setStatus(SessionStatus.FULL);
            }
            entry.setStatus(WaitListStatus.BOOKED);
            waitlistRepository.save(entry);

            eventPublisher.publishEvent(new BookingNotificationEvent(
                    saved.getBookingId(), saved.getUserId(), saved.getUserEmail(), session.getId(),
                    BookingNotificationType.WAITLIST_ALLOCATED));
        }
    }
}
