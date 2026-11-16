package com.bookngo.authservice.service;

import com.bookngo.authservice.dto.response.InternalUserResponse;
import com.bookngo.authservice.exception.BusinessConflictsException;
import com.bookngo.authservice.exception.InformationNotFoundException;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.UserStatus;
import com.bookngo.authservice.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalUserService {

    private final UsersRepository usersRepository;
    private final AuditLogService auditLogService;

    public InternalUserService(UsersRepository usersRepository, AuditLogService auditLogService) {
        this.usersRepository = usersRepository;
        this.auditLogService = auditLogService;
    }

    public InternalUserResponse getUser(Long id) {
        return toResponse(find(id));
    }

    // used by compliance-service (strikes, penalties)
    @Transactional
    public InternalUserResponse updateStatus(Long id, UserStatus newStatus) {
        User user = find(id);
        if (newStatus == UserStatus.PENDING_VERIFICATION) {
            throw new BusinessConflictsException("Status cannot be set to PENDING_VERIFICATION.");
        }
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.DEACTIVATED) {
            throw new BusinessConflictsException("Only ACTIVE or DEACTIVATED users can change status here.");
        }
        UserStatus old = user.getStatus();
        if (old != newStatus) {
            user.setStatus(newStatus);
            usersRepository.save(user);
            auditLogService.logAction("USER_STATUS_CHANGED", "SYSTEM", "User", id,
                    "Status changed from " + old + " to " + newStatus + " by compliance-service");
        }
        return toResponse(user);
    }

    private User find(Long id) {
        return usersRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("User with ID: " + id + " not found."));
    }

    private InternalUserResponse toResponse(User u) {
        return new InternalUserResponse(u.getId(), u.getEmail(), u.getRole(), u.getStatus());
    }
}
