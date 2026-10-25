package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidTimeSpecificationException;
import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.WaitlistEntry;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.model.response.BookingResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.WaitingListRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;

@Service
public class BookingsService {

    private final BookingsRepository bookingsRepository;
    private final SessionsRepository sessionsRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final WaitingService waitingService;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(BookingsService.class);
    private final CurrentUserService currentUserService;
    private final WaitingListRepository waitingListRepository;

    public BookingsService(BookingsRepository bookingsRepository, SessionsRepository sessionsRepository, EmailService emailService, NotificationService notificationService, AuditLogService auditLogService, WaitingService waitingService, WaitlistEntry entry, EntityManager entityManager, CurrentUserService currentUserService, WaitingListRepository waitingListRepository) {
        this.bookingsRepository = bookingsRepository;
        this.sessionsRepository = sessionsRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.waitingService = waitingService;
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
        this.waitingListRepository = waitingListRepository;
    }


    /**
     * Creates a new booking for the currently authenticated user.
     *
     * @param request booking details including session, participants, and booking type
     * @return the newly created booking response
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        logger.info("Creating booking for session ID: {}", request.getSessionId());
        User user = currentUserService.getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(request.getSessionId())
                .orElseThrow(() -> new InformationNotFoundException("Session with ID: " + request.getSessionId() + " not found."));
        boolean alreadyBooked = bookingsRepository.findByUserIdAndSessionIdAndStatus(user.getId(), session.getId(), BookingStatus.CONFIRMED)
                .isPresent();

        if (alreadyBooked) {
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
        boolean hasWaitingCustomers = waitingListRepository.existsBySessionIdAndStatus(session.getId(), WaitListStatus.WAITING);

        if (hasWaitingCustomers) {
            throw new BusinessConflictsException("This session has an active waitlist. Available seats must be allocated to waiting customers first.");
        }

            session.setSpotsLeft(session.getSpotsLeft() - request.getParticipants());
            if (session.getSpotsLeft() == 0) {
                session.setStatus(SessionStatus.FULL);
            }

            sessionsRepository.save(session);

            BigDecimal pricePerPerson = session.getActivity().getPricePerPerson();
            BigDecimal subtotal = pricePerPerson.multiply(BigDecimal.valueOf(request.getParticipants()));

            Bookings booking = new Bookings();
            booking.setUser(user);
            booking.setCreatedBy(user.getEmail());
            booking.setUpdatedBy(user.getEmail());
            booking.setSession(session);
            booking.setParticipants(request.getParticipants());
            booking.setBookingType(request.getBookingType());
            booking.setDiscountPercent(BigDecimal.ZERO);
            booking.setSubtotal(subtotal);
            booking.setTotalPrice(subtotal);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

            Bookings saved = bookingsRepository.save(booking);
            logger.info("Booking created successfully with ID: {} for session ID: {}", saved.getBookingId(), saved.getSession().getId());

            emailService.sendBookingVerifiedEmail(saved.getUser().getEmail(), saved);
            notificationService.create(
                    saved.getUser(),
                    "booking-confirmed",
                    "Booking #" + saved.getBookingId() + " was confirmed successfully."
            );

        return toResponse(saved);
    }


    public BookingResponse getById(Long id) {
        logger.info("Fetching booking with ID: {}", id);
        Bookings booking = bookingsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        User currentUser= currentUserService.getCurrentUser();
        checkOwnershipOrAdmin(booking, currentUser);
        return toResponse(booking);
    }


    @Transactional
    public String cancelBooking(Long id) {
        logger.info("Cancelling booking with ID: {}", id);
        Bookings booking = bookingsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        User currentUser = currentUserService.getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(booking.getSession().getId())
                .orElseThrow(() -> new InformationNotFoundException("Session not found."));

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
        booking.setUpdatedBy(currentUser.getEmail());
        bookingsRepository.save(booking);

        session.setSpotsLeft(session.getSpotsLeft() + booking.getParticipants());
        if (session.getStatus() == SessionStatus.FULL) {
            session.setStatus(SessionStatus.SCHEDULED);
        }
        waitingService.allocateNextWaitlistedUser(session);
        sessionsRepository.save(session);

        auditLogService.logAction("BOOKING_CANCELLED", currentUserService.getCurrentUser().getEmail(), "Booking", booking.getBookingId(), "booking was cancelled by user successfully");

        emailService.sendSessionCancelledEmail(booking.getUser().getEmail(), booking);
        return "Booking with id:" + id + " has been cancelled successfully.";
    }

    private void checkOwnershipOrAdmin(Bookings booking, User currentUser) {
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            logger.warn("Unauthorized access attempt for booking ID: {} by user ID: {}", booking.getBookingId(), currentUser.getId());
            throw new AccessDeniedException("You are not authorized to access this booking.");
        }
    }

    private BookingResponse toResponse(Bookings booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getSession().getId(),
                booking.getParticipants(),
                booking.getBookingType(),
                booking.getSubtotal(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getCreatedAt()
        );
    }
    /**
     * Retrieves the authenticated user's bookings with optional status filtering,
     * pagination, and sorting.
     *
     * @param status optional booking status filter
     * @param pageable pagination and sorting information
     * @return a page of the user's booking responses
     */
    public Page<BookingResponse> getMyBookings(BookingStatus status, Pageable pageable) {
        User user = currentUserService.getCurrentUser();
        return bookingsRepository.findMyBookings(user.getId(), status, pageable).map(this::toResponse);
    }
}