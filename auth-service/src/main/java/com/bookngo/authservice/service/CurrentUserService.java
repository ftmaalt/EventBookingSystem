package com.bookngo.authservice.service;

import com.bookngo.catalogservice.model.model.User;
import com.bookngo.authservice.repository.UsersRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UsersRepository usersRepository;

    public CurrentUserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public User getCurrentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())){
            throw new AuthenticationCredentialsNotFoundException("Authentication Required");
        }

        User loggedInUser = usersRepository.findUserByEmail(auth.getName()).orElseThrow();

        if (loggedInUser == null){
            throw new AuthenticationCredentialsNotFoundException("Authenticated user not found.");
        }

        return loggedInUser;
    }
}
