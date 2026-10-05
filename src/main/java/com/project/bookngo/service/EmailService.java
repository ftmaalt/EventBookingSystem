package com.project.bookngo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        System.out.println("SERVICE Calling sendVerificationEmail==>");
        SimpleMailMessage verificationMessage = new SimpleMailMessage();
        verificationMessage.setTo(toEmail);
        verificationMessage.setSubject("Verify your BooknGo email");
        verificationMessage.setText(
                "Hello," +
                        "\nHere is your verification link:\n" + baseUrl + "/api/auth/verify?token=" + token+
                        "\n\nThis email verification link will expire after 24 hours. If you did not create an account on BooknGo, no further action is required.\n" +
                        "\n" +
                        "Regards,\n" +
                        "BooknGo Team");

        mailSender.send(verificationMessage);
    }

    public void sendPasswordResetEmail(String toEmail, String token){
        System.out.println("SERVICE Calling sendPasswordResetEmail");
        SimpleMailMessage passwordResetMessage= new SimpleMailMessage();
        passwordResetMessage.setTo(toEmail);
        passwordResetMessage.setSubject("Reset your BooknGo account password");
        passwordResetMessage.setText("Hello," +
                "\nYou are receiving this email because we received a password reset request for your account." + "Use this link to reset it: "  + baseUrl + "/api/auth/reset-password?token=" + token +
                "\n\nThis password reset link will expire in 6 minutes.If you did not request a password reset, no further action is required.\n" +
                "\n" +
                "Regards,\n" +
                "BooknGo Team");
    }
}