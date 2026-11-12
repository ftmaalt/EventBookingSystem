package com.bookngo.bookingservice.service;

import com.bookngo.bookingservice.client.ActivityInfo;
import com.bookngo.bookingservice.client.CatalogClient;
import com.bookngo.bookingservice.dto.request.SessionRequest;
import com.bookngo.bookingservice.dto.response.SessionResponse;
import com.bookngo.bookingservice.exception.BusinessConflictsException;
import com.bookngo.bookingservice.exception.InformationNotFoundException;
import com.bookngo.bookingservice.exception.InvalidTimeSpecificationException;
import com.bookngo.bookingservice.model.Sessions;
import com.bookngo.bookingservice.model.enums.SessionStatus;
import com.bookngo.bookingservice.repository.SessionsRepository;
import com.bookngo.bookingservice.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionsService {

    private static final Logger logger = LoggerFactory.getLogger(SessionsService.class);

    private final SessionsRepository sessionsRepository;
    private final CatalogClient catalogClient;
    private final CurrentUserService currentUserService;

    public SessionsService(SessionsRepository sessionsRepository, CatalogClient catalogClient,
                           CurrentUserService currentUserService) {
        this.sessionsRepository = sessionsRepository;
        this.catalogClient = catalogClient;
        this.currentUserService = currentUserService;
    }

    private void checkOwnership(Sessions session) {
        if (!session.getProviderId().equals(currentUserService.getCurrentUser().id())) {
            throw new AccessDeniedException("You are not authorized to modify this activity.");
        }
    }

    public SessionResponse createSession(SessionRequest request) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        // The only synchronous call to another service: validate the activity and copy what we need.
        ActivityInfo activity = catalogClient.getActivity(request.getActivity_id());
        if (!activity.providerId().equals(user.id())) {
            throw new AccessDeniedException("You are not authorized to modify this activity.");
        }
        if (!"ACTIVE".equals(activity.status())) {
            throw new BusinessConflictsException("This activity is not active.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new InvalidTimeSpecificationException("Session start time must be before end time.");
        }

        Sessions session = new Sessions();
        session.setActivityId(activity.activityId());
        session.setActivityTitle(activity.title());
        session.setProviderId(activity.providerId());
        session.setPricePerPerson(activity.pricePerPerson());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCapacity(request.getCapacity());
        session.setSpotsLeft(request.getCapacity());
        session.setStatus(SessionStatus.SCHEDULED);

        Sessions saved = sessionsRepository.save(session);
        logger.info("Session {} created for activity {}", saved.getId(), saved.getActivityId());
        return toResponse(saved);
    }

    public SessionResponse getById(Long id) {
        return toResponse(sessionsRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Session with the id:" + id + " does not exist.")));
    }

    public List<SessionResponse> getAllByActivityId(Long activityId) {
        catalogClient.getActivity(activityId); // 404 if the activity does not exist
        return sessionsRepository.findByActivityId(activityId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest request) {
        Sessions session = sessionsRepository.findByIdForUpdate(id).orElseThrow(() ->
                new InformationNotFoundException("Session with the id:" + id + " does not exist."));
        checkOwnership(session);

        if (session.getStatus() == SessionStatus.IN_PROGRESS || session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {
            throw new InvalidTimeSpecificationException("Cannot modify a session that is in progress, completed, or cancelled.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new InvalidTimeSpecificationException("Session start time must be before end time.");
        }
        if (LocalDateTime.now().plusHours(24).isAfter(session.getStartTime())) {
            throw new InvalidTimeSpecificationException("Cannot update session: start time must be at least 24 hours from now.");
        }
        if (!request.getCapacity().equals(session.getCapacity()) && !session.getSpotsLeft().equals(session.getCapacity())) {
            throw new BusinessConflictsException("Cannot change capacity once bookings exist for this session.");
        }

        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        if (session.getSpotsLeft().equals(session.getCapacity())) {
            session.setCapacity(request.getCapacity());
            session.setSpotsLeft(request.getCapacity());
        }
        return toResponse(sessionsRepository.save(session));
    }

    @Transactional
    public SessionResponse updateSessionStatus(Long id) {
        Sessions session = sessionsRepository.findByIdForUpdate(id).orElseThrow(() ->
                new InformationNotFoundException("Session with the id:" + id + " does not exist."));
        checkOwnership(session);

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(session.getEndTime())) {
            session.setStatus(SessionStatus.COMPLETED);
        } else if (!now.isBefore(session.getStartTime())) {
            session.setStatus(SessionStatus.IN_PROGRESS);
        } else if (session.getSpotsLeft() == 0) {
            session.setStatus(SessionStatus.FULL);
        } else {
            session.setStatus(SessionStatus.SCHEDULED);
        }
        return toResponse(sessionsRepository.save(session));
    }

    @Transactional
    public String cancelSession(Long id) {
        Sessions session = sessionsRepository.findByIdForUpdate(id).orElseThrow(() ->
                new InformationNotFoundException("Session with ID: " + id + " not found."));
        checkOwnership(session);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessConflictsException("Session is already cancelled.");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessConflictsException("Cannot cancel a session that has already started.");
        }
        if (!session.getSpotsLeft().equals(session.getCapacity())) {
            throw new BusinessConflictsException("Cannot cancel a session with active bookings.");
        }
        session.setStatus(SessionStatus.CANCELLED);
        sessionsRepository.save(session);
        return "Session with ID: " + id + " has been cancelled.";
    }

    private SessionResponse toResponse(Sessions s) {
        return new SessionResponse(s.getId(), s.getActivityId(), s.getStartTime(), s.getEndTime(),
                s.getCapacity(), s.getSpotsLeft(), s.getStatus());
    }
}
