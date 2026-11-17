package com.bookngo.complianceservice.service;

import com.bookngo.complianceservice.client.AuthClient;
import com.bookngo.complianceservice.client.BookingClient;
import com.bookngo.complianceservice.client.CancelledSessionsInfo;
import com.bookngo.complianceservice.client.SessionOwnerInfo;
import com.bookngo.complianceservice.dto.request.ReportViolationRequest;
import com.bookngo.complianceservice.dto.response.GenericMessageResponse;
import com.bookngo.complianceservice.model.Penalty;
import com.bookngo.complianceservice.model.enums.PenaltyStatus;
import com.bookngo.complianceservice.model.Violation;
import com.bookngo.complianceservice.model.ViolationCounter;
import com.bookngo.complianceservice.model.enums.ViolationType;
import com.bookngo.complianceservice.repository.PenaltiesRepository;
import com.bookngo.complianceservice.repository.ViolationCounterRepository;
import com.bookngo.complianceservice.repository.ViolationsRepository;
import com.bookngo.complianceservice.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ViolationsService {

    private static final Logger logger = LoggerFactory.getLogger(ViolationsService.class);

    private static final String ACTIVE = "ACTIVE";
    private static final String DEACTIVATED = "DEACTIVATED";
    private static final String BLACKLISTED = "BLACKLISTED";

    private final ViolationsRepository violationsRepository;
    private final PenaltiesRepository penaltiesRepository;
    private final ViolationCounterRepository counterRepository;
    private final AuthClient authClient;
    private final BookingClient bookingClient;
    private final CurrentUserService currentUserService;

    @Value("${app.penalty.fee}")
    private BigDecimal penaltyFee;

    public ViolationsService(ViolationsRepository violationsRepository, PenaltiesRepository penaltiesRepository,
                             ViolationCounterRepository counterRepository, AuthClient authClient,
                             BookingClient bookingClient, CurrentUserService currentUserService) {
        this.violationsRepository = violationsRepository;
        this.penaltiesRepository = penaltiesRepository;
        this.counterRepository = counterRepository;
        this.authClient = authClient;
        this.bookingClient = bookingClient;
        this.currentUserService = currentUserService;
    }

    /**
     * Records a violation and applies the strike rules.
     * Not a distributed transaction: if a later call to auth fails, this transaction is rolled back,
     * but a call that already succeeded earlier is not undone (see README "limitations").
     */
    @Transactional
    public GenericMessageResponse reportViolation(ReportViolationRequest request) {
        AuthenticatedUser reporter = currentUserService.getCurrentUser();
        authClient.getUser(request.getUserId()); // 404 if the offender does not exist

        if ("PROVIDER".equals(reporter.role())) {
            if (request.getSessionId() == null) {
                throw new AccessDeniedException("You can only report violations on your own sessions.");
            }
            SessionOwnerInfo session = bookingClient.getSessionOwner(request.getSessionId());
            if (!session.providerId().equals(reporter.id())) {
                throw new AccessDeniedException("You can only report violations on your own sessions.");
            }
        }

        Violation violation = new Violation();
        violation.setType(request.getType());
        violation.setExcused(request.getExcused());
        violation.setNote(request.getNote());
        violation.setBookingId(request.getBookingId());
        violation.setSessionId(request.getSessionId());
        violation.setUserId(request.getUserId());
        violation.setReportedBy(reporter.id());
        violationsRepository.save(violation);

        if (Boolean.FALSE.equals(request.getExcused())) {
            ViolationCounter counter = lockedCounter(request.getUserId());
            if (request.getType() == ViolationType.PROVIDER_CANCELLATION) {
                handleProviderStrike(counter);
            } else {
                handleCustomerStrike(counter);
            }
        }
        logger.info("Violation {} recorded for user {} by {}", request.getType(), request.getUserId(), reporter.id());
        return new GenericMessageResponse("Violation recorded.");
    }

    private ViolationCounter lockedCounter(Long userId) {
        return counterRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> counterRepository.saveAndFlush(new ViolationCounter(userId)));
    }

    private void handleCustomerStrike(ViolationCounter counter) {
        counter.setConsecutiveViolations((short) (counter.getConsecutiveViolations() + 1));
        // first two strikes need 3 violations in a row; after that a single violation is a strike
        int threshold = counter.getStrikeCount() < 2 ? 3 : 1;
        if (counter.getConsecutiveViolations() >= threshold) {
            issueStrike(counter);
        } else {
            counterRepository.save(counter);
        }
    }

    private void handleProviderStrike(ViolationCounter counter) {
        counter.setConsecutiveViolations((short) (counter.getConsecutiveViolations() + 1));
        if (counter.getConsecutiveViolations() >= 3) {
            issueStrike(counter);
            cancelUpcomingSessions(counter.getUserId());
        } else {
            counterRepository.save(counter);
        }
    }

    private void issueStrike(ViolationCounter counter) {
        short newStrikeCount = (short) (counter.getStrikeCount() + 1);
        counter.setStrikeCount(newStrikeCount);
        counter.setConsecutiveViolations((short) 0);
        counterRepository.save(counter);

        if (newStrikeCount >= 3) {
            authClient.setStatus(counter.getUserId(), BLACKLISTED);
            logger.warn("User {} blacklisted after {} strikes", counter.getUserId(), newStrikeCount);
        } else {
            authClient.setStatus(counter.getUserId(), DEACTIVATED);
            Penalty penalty = new Penalty();
            penalty.setUserId(counter.getUserId());
            penalty.setStrikeNumber(newStrikeCount);
            penalty.setAmount(penaltyFee);
            penalty.setStatus(PenaltyStatus.PENDING);
            penaltiesRepository.save(penalty);
            logger.warn("User {} deactivated with strike {}, penalty of {} issued", counter.getUserId(), newStrikeCount, penaltyFee);
        }
    }

    /** The strike is already decided at this point, so a booking-service outage must not undo it. */
    private void cancelUpcomingSessions(Long providerId) {
        try {
            CancelledSessionsInfo result = bookingClient.cancelUpcomingSessions(providerId);
            logger.info("Provider {} strike: {} sessions and {} bookings cancelled",
                    providerId, result.sessionsCancelled(), result.bookingsCancelled());
        } catch (RuntimeException e) {
            logger.error("Provider {} got a strike but its upcoming sessions could not be cancelled. Cancel them manually.",
                    providerId, e);
        }
    }
}
