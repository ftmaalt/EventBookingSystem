package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.request.UpdateUserRoleRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private UsersRepository usersRepository;

    // User Role and Authorization
    public GenericMessageResponse updateUserRole(Long userId, UpdateUserRoleRequest request){
        System.out.println("SERVICE Calling updateUserRole");
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setRole(request.getRole());
        usersRepository.save(user);
        return new GenericMessageResponse("Your Role has been updated successfully");
    }

    public GenericMessageResponse deactivateUser(Long userId) {
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setStatus(UserStatus.DEACTIVATED);
        usersRepository.save(user);
        return new GenericMessageResponse("User account with ID#"+ userId +" has been deactivated Successfully");
    }

    public GenericMessageResponse activateUser(Long userId) {
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
        return new GenericMessageResponse("User account with ID#"+ userId +" has been reactivated Successfully");
    }
}
