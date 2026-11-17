package com.bookngo.notificationservice.service;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;
import com.bookngo.notificationservice.model.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The single entry point for "notify someone". It does not care how the request arrived
 * (REST today, a Kafka consumer later): it only validates, stores the in-app notification, then e-mails.
 * Deliberately NOT @Transactional: the e-mail goes out after the notification has been committed.
 */
@Service
public class NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public NotificationDispatcher(NotificationService notificationService, EmailService emailService) {
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    public void dispatch(NotificationEventRequest request) {
        validate(request);
        NotificationType type = request.type();
        logger.info("Dispatching {} (userId={}, bookingId={})", type, request.userId(), request.bookingId());

        if (type.isInApp()) {
            notificationService.create(request.userId(), type.code(), MessageTemplates.inApp(request));
        }
        if (hasText(request.email())) {
            MessageTemplates.EmailContent email = MessageTemplates.email(request, frontendUrl);
            emailService.send(request.email(), email.subject(), email.body());
        }
    }

    private void validate(NotificationEventRequest r) {
        NotificationType type = r.type();
        if (type.isInApp() && r.userId() == null) {
            throw new IllegalArgumentException("userId is required for " + type);
        }
        if (!type.isInApp() && !hasText(r.email())) {
            throw new IllegalArgumentException("email is required for " + type);
        }
        if ((type == NotificationType.EMAIL_VERIFICATION || type == NotificationType.PASSWORD_RESET)
                && !hasText(r.token())) {
            throw new IllegalArgumentException("token is required for " + type);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
