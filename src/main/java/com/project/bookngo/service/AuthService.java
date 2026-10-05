package com.project.bookngo.service;

import com.project.bookngo.enums.UserRole;
import com.project.bookngo.enums.UserStatus;
//import com.project.bookngo.enums.TokenType;
import com.project.bookngo.exception.InformationExistsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.exception.VerificationRequiredException;
import com.project.bookngo.model.Tokens;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.TokenType;
import com.project.bookngo.model.request.*;
import com.project.bookngo.model.response.ForgotPasswordResponse;
import com.project.bookngo.model.response.LoginResponse;
import com.project.bookngo.model.response.RegisterResponse;
import com.project.bookngo.repository.UsersRepository;
import com.project.bookngo.security.JwtUtils;
import com.project.bookngo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private JwtUtils jwtUtils;


    // ---registration section---
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

    public RegisterResponse verifyEmail(String token) {
        System.out.println("SERVICE Calling verifyEmail==>");
        User user = tokenService.validateToken(token, TokenType.EMAIL_VERIFICATION);
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
        return new RegisterResponse("Email verification successful, Please log in again to use the app.");
    }

    //    ---login section---
    public LoginResponse login(LoginRequest loginRequest) {
        System.out.println("SERVICE Calling login");
        User loginAttemptUser= usersRepository.findUserByEmail(loginRequest.getEmail());
        if (loginAttemptUser == null) {
            throw new InformationNotFoundException("The Email/Password you entered is not correct. Please try again.");
        }
        if (loginAttemptUser.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new VerificationRequiredException("Please verify your email before attempting to login.");

        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), loginAttemptUser.getPasswordHash())) {
            throw new InvalidCredentials("The Email/Password you entered is not correct. Please try again.");
        }
        MyUserDetails userDetails= new MyUserDetails(loginAttemptUser);
       final String token= jwtUtils.generateJwtToken(userDetails);
        return new LoginResponse(token, loginAttemptUser.getRole().toString());
    }

//    --- Reset Password ---
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        User user = usersRepository.findUserByEmail(forgotPasswordRequest.getEmail());
        if (user != null) {
            String resetToken = tokenService.generateToken(user, TokenType.PASSWORD_RESET);
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        }
            // sending a valid message without actually sending the email to ensure that no data leak happen
    return new ForgotPasswordResponse("An message was sent to the email with the password reset link.");
    }
    public ForgotPasswordResponse resetPassword(ResetPasswordRequest passwordRequest){
        User user = tokenService.validateToken(passwordRequest.getToken(), TokenType.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(passwordRequest.getNewPassword()));
        usersRepository.save(user);
        return new ForgotPasswordResponse("Password Reset Successful. You can now log in using your new password.");

    }
//    --- Change Password ---
public ForgotPasswordResponse changePassword(ChangePasswordRequest request) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    User user= usersRepository.findUserByEmail(email);
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())){
            throw new InvalidCredentials("The Password you entered is not correct. Please try again.");
    }else{
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usersRepository.save(user);
        return new ForgotPasswordResponse("Password Changed Successfully.");
    }
}

// User Role and Authorization
    public RegisterResponse updateUserRole(Long userId, UpdateUserRoleRequest request){
        User user= usersRepository.findById(userId).orElseThrow(() -> new InformationNotFoundException("The Email/Password you entered is not correct. Please try again."));
        user.setRole(request.getRole());
        usersRepository.save(user);
        return new RegisterResponse("Your Role has been updated successfully");
    }


}
