package com.bookngo.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** e-mail problem must never lose the in-app notification*/
    public void send(String to, String subject, String body) {
        if (!enabled) {
            logger.info("E-mail disabled (app.mail.enabled=false), skipping '{}' to {}", subject, to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("E-mail '{}' sent to {}", subject, to);
        } catch (Exception e) {
            logger.error("Failed to send e-mail '{}' to {}", subject, to, e);
        }
    }
}
