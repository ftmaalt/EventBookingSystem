package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidTimeSpecificationException;
import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.enums.SessionStatus;
import com.project.bookngo.model.request.SessionRequest;
import com.project.bookngo.model.response.SessionResponse;
import com.project.bookngo.repository.ActivitiesRepository;
import com.project.bookngo.repository.SessionsRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionsService {

    @Autowired
    private SessionsRepository sessionsRepository;
    @Autowired
    private ActivitiesRepository activitiesRepository;
    @Autowired
    private CurrentUserService currentUserService;

    private static final Logger logger = LoggerFactory.getLogger(SessionsService.class);


    private void checkOwnership(Activities activity) {
        if (!activity.getProvider().getId().equals(currentUserService.getCurrentUser().getId())) {
            throw new AccessDeniedException("You are not authorized to modify this activity.");
        }
    }

    public SessionResponse createSession(SessionRequest request) {
        logger.info("Creating session for activity ID: {}", request.getActivity_id());
        Activities activity = activitiesRepository.findById(request.getActivity_id())
                .orElseThrow(() -> new InformationNotFoundException("Activity with ID: " + request.getActivity_id() + " not found."));

        checkOwnership(activity);

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new InvalidTimeSpecificationException("Session start time must be before end time.");
        }

        Sessions session = new Sessions();
        session.setActivity(activity);
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCapacity(request.getCapacity());
        session.setSpotsLeft(request.getCapacity());
        session.setStatus(SessionStatus.SCHEDULED);

        Sessions saved = sessionsRepository.save(session);
        logger.info("Session created successfully with ID: {}", saved.getId());
        return toResponse(saved);
    }

    public SessionResponse getById(Long id) {
        logger.info("Fetching session with ID: {}", id);
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with the id:" + id + " does not exist."));
        return toResponse(session);
    }

    public List<SessionResponse> getAllByActivityId(Long activityId) {
        logger.info("Fetching sessions for activity ID: {}", activityId);
        if (!activitiesRepository.existsById(activityId)) {
            throw new InformationNotFoundException("Activity with ID: " + activityId + " not found.");
        }
        return sessionsRepository.findByActivityId(activityId).stream().map(this::toResponse).toList();
    }

    public SessionResponse updateSession(Long id, SessionRequest request) {
        logger.info("Updating session with ID: {}", id);
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with the id:" + id + " does not exist."));

        checkOwnership(session.getActivity());

        if (session.getStatus() == SessionStatus.IN_PROGRESS || session.getStatus() == SessionStatus.COMPLETED) {
            throw new InvalidTimeSpecificationException("Cannot update a session that is in progress or completed.");
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
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getSpotsLeft().equals(session.getCapacity())) {
            session.setCapacity(request.getCapacity());
            session.setSpotsLeft(request.getCapacity());
        }

        Sessions updated = sessionsRepository.save(session);
        logger.info("Session with ID {} updated successfully", id);
        return toResponse(updated);
    }

    public SessionResponse updateSessionStatus(Long id) {
        logger.info("Updating status for session with ID: {}", id);
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with the id:" + id + " does not exist."));
        if (session.getStatus() == SessionStatus.CANCELLED) {
            return toResponse(session);
        }

        if (session.getSpotsLeft() == 0) {
            session.setStatus(SessionStatus.FULL);
        } else if (!LocalDateTime.now().isBefore(session.getStartTime()) && LocalDateTime.now().isBefore(session.getEndTime())) {
            session.setStatus(SessionStatus.IN_PROGRESS);
        } else if (!LocalDateTime.now().isBefore(session.getEndTime())) {
            session.setStatus(SessionStatus.COMPLETED);
        }

        Sessions saved = sessionsRepository.save(session);
        logger.info("Session with ID {} status updated successfully", id);
        return toResponse(saved);
    }


    @Transactional
    public String cancelSession(Long id) {
        Sessions session = sessionsRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Session with ID: " + id + " not found."));

        checkOwnership(session.getActivity());

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessConflictsException("Session is already cancelled.");
        }

        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessConflictsException("Cannot cancel a session that has already started.");
        }

        boolean hasBookings =
                !session.getSpotsLeft().equals(session.getCapacity());

        if (hasBookings) {
            throw new BusinessConflictsException("Cannot cancel a session with active bookings.");
        }

        session.setStatus(SessionStatus.CANCELLED);
        sessionsRepository.save(session);

        logger.info("Session with ID {} cancelled successfully", id);

        return "Session with ID: " + id + " has been cancelled.";
    }


    private SessionResponse toResponse(Sessions session) {
        return new SessionResponse(
                session.getId(),
                session.getActivity().getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getSpotsLeft(),
                session.getStatus()
        );
    }
}