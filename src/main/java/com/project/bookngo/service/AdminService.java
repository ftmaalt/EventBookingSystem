package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.request.UpdateUserRoleRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class AdminService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuditLogService auditLogService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // User Role and Authorization
    public GenericMessageResponse updateUserRole(Long userId, UpdateUserRoleRequest request){
        logger.info("Updating role for user ID: {}", userId);
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setRole(request.getRole());
        usersRepository.save(user);
        logger.info("Role updated successfully for user ID: {}", userId);
        return new GenericMessageResponse("Your Role has been updated successfully");
    }

    public GenericMessageResponse deactivateUser(Long userId) {
        logger.info("Deactivating user ID: {}", userId);
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setStatus(UserStatus.DEACTIVATED);
        auditLogService.logAction("USER_DEACTIVATED", getCurrentUser().getEmail(), "User", user.getId() , "User account was deactivated by admin");
        usersRepository.save(user);
        logger.info("User ID {} has been deactivated", userId);
        return new GenericMessageResponse("User account with ID#"+ userId +" has been deactivated Successfully");
    }

    public GenericMessageResponse activateUser(Long userId) {
        logger.info("Activating user ID: {}", userId);
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
        logger.info("User ID {} has been activated", userId);
        return new GenericMessageResponse("User account with ID#"+ userId +" has been reactivated Successfully");
    }
}
