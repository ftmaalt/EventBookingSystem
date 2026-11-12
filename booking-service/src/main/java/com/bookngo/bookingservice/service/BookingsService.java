package com.bookngo.bookingservice.service;

import com.bookngo.bookingservice.dto.request.BookingRequest;
import com.bookngo.bookingservice.dto.response.BookingResponse;
import com.bookngo.bookingservice.event.BookingNotificationEvent;
import com.bookngo.bookingservice.event.BookingNotificationType;
import com.bookngo.bookingservice.exception.BusinessConflictsException;
import com.bookngo.bookingservice.exception.InformationNotFoundException;
import com.bookngo.bookingservice.exception.InvalidTimeSpecificationException;
import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.Sessions;
import com.bookngo.bookingservice.model.enums.*;
import com.bookngo.bookingservice.repository.BookingsRepository;
import com.bookngo.bookingservice.repository.SessionsRepository;
import com.bookngo.bookingservice.repository.WaitingListRepository;
import com.bookngo.bookingservice.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BookingsService {

    private static final Logger logger = LoggerFactory.getLogger(BookingsService.class);

    private final BookingsRepository bookingsRepository;
    private final SessionsRepository sessionsRepository;
    private final WaitingService waitingService;
    private final WaitingListRepository waitingListRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public BookingsService(BookingsRepository bookingsRepository, SessionsRepository sessionsRepository,
                           WaitingService waitingService, WaitingListRepository waitingListRepository,
                           CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher,
                           EntityManager entityManager) {
        this.bookingsRepository = bookingsRepository;
        this.sessionsRepository = sessionsRepository;
        this.waitingService = waitingService;
        this.waitingListRepository = waitingListRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    /** Creates a booking. The session row is locked first so concurrent requests are handled one at a time. */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(request.getSessionId()).orElseThrow(() ->
                new InformationNotFoundException("Session with ID: " + request.getSessionId() + " not found."));

        if (bookingsRepository.findByUserIdAndSessionIdAndStatus(user.id(), session.getId(), BookingStatus.CONFIRMED).isPresent()) {
            throw new BusinessConflictsException("You already have a booking for this session.");
        }
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new InvalidTimeSpecificationException("This session is no longer available for booking.");
        }
        if (session.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidTimeSpecificationException("Cannot book a session that has already started.");
        }
        if (session.getSpotsLeft() < request.getParticipants()) {
            throw new BusinessConflictsException("Not enough spots left. Only " + session.getSpotsLeft() + " remaining.");
        }
        if (waitingListRepository.existsBySessionIdAndStatus(session.getId(), WaitListStatus.WAITING)) {
            throw new BusinessConflictsException("This session has an active waitlist. Available seats must be allocated to waiting customers first.");
        }

        session.setSpotsLeft(session.getSpotsLeft() - request.getParticipants());
        if (session.getSpotsLeft() == 0) {
            session.setStatus(SessionStatus.FULL);
        }
        sessionsRepository.save(session);

        BigDecimal subtotal = session.getPricePerPerson().multiply(BigDecimal.valueOf(request.getParticipants()));
        Bookings booking = new Bookings();
        booking.setUserId(user.id());
        booking.setUserEmail(user.email());
        booking.setCreatedBy(user.email());
        booking.setUpdatedBy(user.email());
        booking.setSession(session);
        booking.setParticipants(request.getParticipants());
        booking.setBookingType(request.getBookingType());
        booking.setDiscountPercent(BigDecimal.ZERO);
        booking.setSubtotal(subtotal);
        booking.setTotalPrice(subtotal);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        Bookings saved = bookingsRepository.save(booking);

        eventPublisher.publishEvent(new BookingNotificationEvent(
                saved.getBookingId(), saved.getUserId(), saved.getUserEmail(), session.getId(),
                BookingNotificationType.BOOKING_CONFIRMED));
        logger.info("Booking {} created for user {} on session {}", saved.getBookingId(), user.id(), session.getId());
        return toResponse(saved);
    }

    public BookingResponse getById(Long id) {
        Bookings booking = bookingsRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        checkOwnershipOrAdmin(booking, currentUserService.getCurrentUser());
        return toResponse(booking);
    }

    @Transactional
    public String cancelBooking(Long id) {
        Bookings booking = bookingsRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        AuthenticatedUser currentUser = currentUserService.getCurrentUser();

        // Lock the session, then re-read the booking so we see any change committed while we waited.
        Sessions session = sessionsRepository.findByIdForUpdate(booking.getSession().getId()).orElseThrow(() ->
                new InformationNotFoundException("Session not found."));
        entityManager.refresh(booking);

        checkOwnershipOrAdmin(booking, currentUser);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessConflictsException("This booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessConflictsException("Cannot cancel a completed booking.");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessConflictsException("Cannot cancel a booking after the session has started.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedBy(currentUser.email());
        bookingsRepository.save(booking);

        eventPublisher.publishEvent(new BookingNotificationEvent(
                booking.getBookingId(), booking.getUserId(), booking.getUserEmail(), session.getId(),
                BookingNotificationType.BOOKING_CANCELLED));

        session.setSpotsLeft(session.getSpotsLeft() + booking.getParticipants());
        if (session.getStatus() == SessionStatus.FULL) {
            session.setStatus(SessionStatus.SCHEDULED);
        }
        waitingService.allocateNextWaitlistedUser(session);
        sessionsRepository.save(session);

        logger.info("AUDIT BOOKING_CANCELLED bookingId={} by={}", booking.getBookingId(), currentUser.email());
        return "Booking with id:" + id + " has been cancelled successfully.";
    }

    public Page<BookingResponse> getMyBookings(BookingStatus status, Pageable pageable) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        return bookingsRepository.findMyBookings(user.id(), status, pageable).map(this::toResponse);
    }

    private void checkOwnershipOrAdmin(Bookings booking, AuthenticatedUser user) {
        boolean isOwner = booking.getBookingId().equals(user.id());
        boolean isAdmin = "ADMIN".equals(user.role());
        if (!isOwner && !isAdmin) {
            logger.warn("Unauthorized access attempt for booking {} by user {}", booking.getBookingId(), user.id());
            throw new AccessDeniedException("You are not authorized to access this booking.");
        }
    }

    private BookingResponse toResponse(Bookings b) {
        return new BookingResponse(b.getBookingId(), b.getSession().getId(), b.getParticipants(), b.getBookingType(),
                b.getSubtotal(), b.getTotalPrice(), b.getStatus(), b.getPaymentStatus(), b.getCreatedAt());
    }
}
