package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.exception.InvalidTimeSpecificationException;
import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.SessionRequest;
import com.project.bookngo.model.response.SessionResponse;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.ActivitiesRepository;
import com.project.bookngo.repository.SessionsRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionsService {

    @Autowired private SessionsRepository sessionsRepository;
    @Autowired private ActivitiesRepository activitiesRepository;
    @Autowired private UsersRepository usersRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    private void checkOwnership(Activities activity) {
        if (!activity.getProvider().getId().equals(getCurrentUser().getId())) {
            throw new InvalidCredentials("You are not authorized to modify this activity.");
        }
    }

    public SessionResponse createSession(SessionRequest request) {
        System.out.println("SERVICE Calling createSession ==>");
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
        return toResponse(saved);
    }

    public SessionResponse getById(Long id) {
        System.out.println("SERVICE Calling getById ==>");
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with the id:" + id + " does not exist."));
        return toResponse(session);
    }

    public List<SessionResponse> getAllByActivityId(Long activityId) {
        System.out.println("SERVICE Calling getAllByActivityId ==>");
        if (!activitiesRepository.existsById(activityId)) {
            throw new InformationNotFoundException("Activity with ID: " + activityId + " not found.");
        }
        return sessionsRepository.findByActivityId(activityId).stream().map(this::toResponse).toList();
    }

    public SessionResponse updateSession(Long id, SessionRequest request) {
        System.out.println("SERVICE Calling updateSession ==>");
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
        // TODO:only block capacity changes if bookings already exist (spotsLeft has moved from capacity)
        if (!request.getCapacity().equals(session.getCapacity()) && !session.getSpotsLeft().equals(session.getCapacity())) {
            throw new IllegalArgumentException("Cannot change capacity once bookings exist for this session.");
        }

        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getSpotsLeft().equals(session.getCapacity())) {
            // TODO:Update For booking.. safe to update both together
            session.setCapacity(request.getCapacity());
            session.setSpotsLeft(request.getCapacity());
        }

        Sessions updated = sessionsRepository.save(session);
        return toResponse(updated);
    }

    public SessionResponse updateSessionStatus(Long id) {
        System.out.println("SERVICE Calling updateSessionStatus ==>");
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with the id:" + id + " does not exist."));

        if (session.getSpotsLeft() == 0) {
            session.setStatus(SessionStatus.FULL);
        } else if (!LocalDateTime.now().isBefore(session.getStartTime()) && LocalDateTime.now().isBefore(session.getEndTime())) {
            session.setStatus(SessionStatus.IN_PROGRESS);
        } else if (!LocalDateTime.now().isBefore(session.getEndTime())) {
            session.setStatus(SessionStatus.COMPLETED);
        }

        Sessions saved = sessionsRepository.save(session);
        return toResponse(saved);
    }

    public String cancelSession(Long id) {
        System.out.println("SERVICE Calling cancelSession ==>");
        Sessions session = sessionsRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Session with ID: " + id + " not found."));

        checkOwnership(session.getActivity());

        boolean hasBookings = !session.getSpotsLeft().equals(session.getCapacity());
        if (hasBookings) {
            throw new IllegalArgumentException("Cannot delete a session with active bookings. Cancel it instead.");
        }

        sessionsRepository.delete(session);
        return "Session with id:" + id + " has been deleted successfully.";
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