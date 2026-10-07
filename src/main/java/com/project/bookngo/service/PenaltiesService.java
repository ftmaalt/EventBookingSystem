package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.model.Penalties;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.PenaltyStatus;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.repository.PenaltiesRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PenaltiesService {

    @Autowired
    private PenaltiesRepository penaltiesRepository;
    @Autowired
    private UsersRepository usersRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public GenericMessageResponse payPenalty(Long id) {
        Penalties penalty = penaltiesRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Penalty with ID: " + id + " not found."));

        User current = getCurrentUser();
        boolean isOwner = penalty.getUser().getId().equals(current.getId());
        boolean isAdmin = current.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new InvalidCredentials("You are not authorized to pay this penalty.");
        }

        if (penalty.getStatus() == PenaltyStatus.PAID) {
            throw new IllegalArgumentException("This penalty has already been paid.");
        }

        penalty.setStatus(PenaltyStatus.PAID);
        penalty.setPaidAt(LocalDateTime.now());
        penaltiesRepository.save(penalty);

        User offender = penalty.getUser();
        if (offender.getStatus() == UserStatus.DEACTIVATED) {
            offender.setStatus(UserStatus.ACTIVE);
            usersRepository.save(offender);
        }

        return new GenericMessageResponse("Penalty paid. Account reactivated.");
    }
}