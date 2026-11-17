package com.bookngo.complianceservice.service;

import com.bookngo.complianceservice.client.AuthClient;
import com.bookngo.complianceservice.client.UserInfo;
import com.bookngo.complianceservice.dto.response.GenericMessageResponse;
import com.bookngo.complianceservice.dto.response.PenaltyResponse;
import com.bookngo.complianceservice.exception.BusinessConflictsException;
import com.bookngo.complianceservice.exception.InformationNotFoundException;
import com.bookngo.complianceservice.model.Penalty;
import com.bookngo.complianceservice.model.enums.PenaltyStatus;
import com.bookngo.complianceservice.repository.PenaltiesRepository;
import com.bookngo.complianceservice.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PenaltiesService {

    private final PenaltiesRepository penaltiesRepository;
    private final AuthClient authClient;
    private final CurrentUserService currentUserService;

    public PenaltiesService(PenaltiesRepository penaltiesRepository, AuthClient authClient,
                            CurrentUserService currentUserService) {
        this.penaltiesRepository = penaltiesRepository;
        this.authClient = authClient;
        this.currentUserService = currentUserService;
    }

    public List<PenaltyResponse> getMine() {
        Long userId = currentUserService.getCurrentUser().id();
        return penaltiesRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GenericMessageResponse payPenalty(Long id) {
        Penalty penalty = penaltiesRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Penalty with ID: " + id + " not found."));
        AuthenticatedUser current = currentUserService.getCurrentUser();

        boolean isOwner = penalty.getUserId().equals(current.id());
        boolean isAdmin = "ADMIN".equals(current.role());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to pay this penalty.");
        }
        if (penalty.getStatus() != PenaltyStatus.PENDING) {
            throw new BusinessConflictsException("This penalty has already been " + penalty.getStatus().name().toLowerCase() + ".");
        }

        penalty.setStatus(PenaltyStatus.PAID);
        penalty.setPaidAt(LocalDateTime.now());
        penaltiesRepository.save(penalty);

        UserInfo offender = authClient.getUser(penalty.getUserId());
        if ("DEACTIVATED".equals(offender.status())) {
            authClient.setStatus(penalty.getUserId(), "ACTIVE");
            return new GenericMessageResponse("Penalty paid. Account reactivated.");
        }
        return new GenericMessageResponse("Penalty paid.");
    }

    private PenaltyResponse toResponse(Penalty p) {
        return new PenaltyResponse(p.getId(), p.getStrikeNumber(), p.getAmount(), p.getStatus().name(),
                p.getCreatedAt(), p.getPaidAt());
    }
}
