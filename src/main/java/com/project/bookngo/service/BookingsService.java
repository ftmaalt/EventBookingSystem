package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.exception.InvalidTimeSpecificationException;
import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.PaymentStatus;
import com.project.bookngo.model.enums.SessionStatus;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.model.response.BookingResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingsService {

    @Autowired
    private BookingsRepository bookingsRepository;
    @Autowired
    private SessionsRepository sessionsRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private SSEService sseService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        System.out.println("SERVICE Calling createBooking ==>");
        User user = getCurrentUser();
        Sessions session = sessionsRepository.findByIdForUpdate(request.getSessionId())
                .orElseThrow(() -> new InformationNotFoundException("Session with ID: " + request.getSessionId() + " not found."));

        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new InvalidTimeSpecificationException("This session is no longer available for booking.");
        }
        if (session.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidTimeSpecificationException("Cannot book a session that has already started.");
        }
        if (session.getSpotsLeft() < request.getParticipants()) {
            throw new IllegalArgumentException("Not enough spots left. Only " + session.getSpotsLeft() + " remaining.");
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
        booking.setSession(session);
        booking.setParticipants(request.getParticipants());
        booking.setBookingType(request.getBookingType());
        booking.setSubtotal(subtotal);
        booking.setTotalPrice(subtotal);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Bookings saved = bookingsRepository.save(booking);

        emailService.sendBookingVerifiedEmail(saved.getUser().getEmail(), saved);
        sseService.sendNotification(saved.getUser().getId(), "booking-confirmed", toResponse(saved));

        return toResponse(saved);
    }

    public BookingResponse getById(Long id) {
        System.out.println("SERVICE Calling getById ==>");
        Bookings booking = bookingsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        checkOwnershipOrAdmin(booking);
        return toResponse(booking);
    }

    public List<BookingResponse> getMyBookings() {
        System.out.println("SERVICE Calling getMyBookings ==>");
        User user = getCurrentUser();
        return bookingsRepository.findByUserId(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public String cancelBooking(Long id) {
        System.out.println("SERVICE Calling cancelBooking ==>");
        Bookings booking = bookingsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Booking with the id:" + id + " does not exist."));
        checkOwnershipOrAdmin(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("This booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed booking.");
        }

        Sessions session = sessionsRepository.findByIdForUpdate(booking.getSession().getId())
                .orElseThrow(() -> new InformationNotFoundException("Session not found."));
        session.setSpotsLeft(session.getSpotsLeft() + booking.getParticipants());
        if (session.getStatus() == SessionStatus.FULL) {
            session.setStatus(SessionStatus.SCHEDULED);
        }
        sessionsRepository.save(session);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingsRepository.save(booking);

        emailService.sendSessionCancelledEmail(booking.getUser().getEmail(), booking);
        return "Booking with id:" + id + " has been cancelled successfully.";
    }

    private void checkOwnershipOrAdmin(Bookings booking) {
        User current = getCurrentUser();
        boolean isOwner = booking.getUser().getId().equals(current.getId());
        boolean isAdmin = current.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new InvalidCredentials("You are not authorized to access this booking.");
        }
    }

    private BookingResponse toResponse(Bookings booking) {
        return new BookingResponse(
                booking.getId(),
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
}