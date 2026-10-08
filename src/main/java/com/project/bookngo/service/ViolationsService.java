package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.request.ReportViolationRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ViolationsService {

    @Autowired
    private ViolationsRepository violationsRepository;

    @Autowired
    private PenaltiesRepository penaltiesRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private SessionsRepository sessionsRepository;

    @Autowired
    private ActivitiesRepository activitiesRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditLogService auditLogService;

    private static final BigDecimal PENALTY_FEE = new BigDecimal("10.000");

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public GenericMessageResponse reportViolation(ReportViolationRequest request) {
        User reporter = getCurrentUser();
        User offender = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new InformationNotFoundException("User with ID: " + request.getUserId() + " not found."));

        Bookings booking = null;
        if (request.getBookingId() != null) {
            booking = bookingsRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new InformationNotFoundException("Booking not found."));
        }
        Sessions session = null;
        if (request.getSessionId() != null) {
            session = sessionsRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new InformationNotFoundException("Session not found."));
        }

        if (reporter.getRole() == UserRole.PROVIDER) {
            if (session == null || !session.getActivity().getProvider().getId().equals(reporter.getId())) {
                throw new InvalidCredentials("You can only report violations on your own sessions.");
            }
        }

        Violations violation = new Violations();
        violation.setType(request.getType());
        violation.setExcused(request.getExcused());
        violation.setNote(request.getNote());
        violation.setBooking(booking);
        violation.setSession(session);
        violation.setUser(offender);
        violationsRepository.save(violation);

        if (Boolean.FALSE.equals(request.getExcused())) {
            if (request.getType() == ViolationType.PROVIDER_CANCELLATION) {
                handleProviderStrike(offender);
            } else {
                handleCustomerStrike(offender);
            }
        }

        return new GenericMessageResponse("Violation recorded.");
    }

    private void handleCustomerStrike(User user) {
        user.setConsecutiveViolations((short) (user.getConsecutiveViolations() + 1));

        int threshold = (user.getStrikeCount() < 2) ? 3 : 1;

        if (user.getConsecutiveViolations() >= threshold) {
            issueStrike(user);
        } else {
            usersRepository.save(user);
        }
    }

    private void handleProviderStrike(User provider) {
        provider.setConsecutiveViolations((short) (provider.getConsecutiveViolations() + 1));

        if (provider.getConsecutiveViolations() >= 3) {
            issueStrike(provider);
            cancelUpcomingSessionsForProvider(provider);
        } else {
            usersRepository.save(provider);
        }
    }

    private void issueStrike(User user) {
        short newStrikeCount = (short) (user.getStrikeCount() + 1);
        user.setStrikeCount(newStrikeCount);
        user.setConsecutiveViolations((short) 0);

        if (newStrikeCount >= 3) {
            user.setStatus(UserStatus.BLACKLISTED);
            auditLogService.logAction("USER_BLACKLISTED", "SYSTEM", "User", user.getId() , "User account was blacklisted due to continuous violations");
            usersRepository.save(user);
        } else {
            user.setStatus(UserStatus.DEACTIVATED);
            usersRepository.save(user);

            Penalties penalty = new Penalties();
            penalty.setUser(user);
            penalty.setStrikeNumber(newStrikeCount);
            penalty.setAmount(PENALTY_FEE);
            penalty.setStatus(PenaltyStatus.PENDING);
            penaltiesRepository.save(penalty);
        }
    }

    private void cancelUpcomingSessionsForProvider(User provider) {
        List<Activities> activities = activitiesRepository.findByProviderId(provider.getId());
        for (Activities activity : activities) {
            List<Sessions> sessions = sessionsRepository.findByActivityId(activity.getId());
            for (Sessions session : sessions) {
                if (session.getStatus() == SessionStatus.SCHEDULED || session.getStatus() == SessionStatus.FULL) {
                    session.setStatus(SessionStatus.CANCELLED);
                    sessionsRepository.save(session);

                    List<Bookings> bookings = bookingsRepository.findBySessionId(session.getId());
                    for (Bookings booking : bookings) {
                        if (booking.getStatus() == BookingStatus.CONFIRMED) {
                            booking.setStatus(BookingStatus.CANCELLED);
                            booking.setPaymentStatus(PaymentStatus.REFUNDED);
                            bookingsRepository.save(booking);
                            emailService.sendSessionCancelledEmail(booking.getUser().getEmail(), booking);
                        }
                    }
                }
            }
        }
    }

}