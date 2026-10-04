package com.project.bookngo.service;

import com.project.bookngo.enums.UserRole;
import com.project.bookngo.enums.UserStatus;
//import com.project.bookngo.enums.TokenType;
import com.project.bookngo.exception.InformationExistsException;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.TokenType;
import com.project.bookngo.model.request.RegisterRequest;
import com.project.bookngo.model.response.RegisterResponse;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private EmailService emailService;

    public RegisterResponse register(RegisterRequest request) {
        System.out.println("SERVICE Calling register==>");
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new InformationExistsException("The email you inputted has already been used. Please try again with another email.");
        } else {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setFullName(request.getFullname());
            user.setPhone(request.getPhone());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.PENDING_VERIFICATION);

            user.setStrikeCount((short) 0);
            user.setConsecutiveViolations((short) 0);
            user.setMustChangePassword(false);

            usersRepository.save(user);

            String emailVerificationToken = tokenService.generateToken(user, TokenType.EMAIL_VERIFICATION);
            emailService.sendVerificationEmail(user.getEmail(), emailVerificationToken);
        return new RegisterResponse("Registration successful. Please Verify your account via email to be able to use the app.");
        }
    }
    public RegisterResponse verifyEmail(String token){
        System.out.println("SERVICE Calling verifyEmail==>");
        User user= tokenService.validateToken(token, TokenType.EMAIL_VERIFICATION);
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
        return new RegisterResponse("Email verification successful, you can now use the app.");
    }
}
