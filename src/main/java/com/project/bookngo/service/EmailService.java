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
        verificationMessage.setSubject("Verify your BookNGo email.");
        verificationMessage.setText(
                "Click the link below to verify your email: " + baseUrl + "/api/auth/verify?token=" + token);
        mailSender.send(verificationMessage);
    }
}