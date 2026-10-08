package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Notification;
import com.project.bookngo.model.User;
import com.project.bookngo.model.response.NotificationResponse;
import com.project.bookngo.repository.NotificationRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SSEService sseService;

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationService.class);

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public NotificationResponse create(
            User user,
            String type,
            String message
    ) {

        logger.info(
                "Creating notification for user ID {} ({})",
                user.getId(),
                user.getEmail()
        );

        Notification notification =
                new Notification();

        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);

        Notification saved =
                notificationRepository.save(notification);

        logger.info(
                "Notification saved successfully. ID: {}, user ID: {}",
                saved.getId(),
                user.getId()
        );

        NotificationResponse response =
                toResponse(saved);

        sseService.sendNotification(
                user.getId(),
                type,
                response
        );

        return response;
    }

    public List<NotificationResponse> getMine() {
        User user = getCurrentUser();
        return notificationRepository
                .findTop30ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAllRead() {
        User user = getCurrentUser();
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadFalse(user.getId());

        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void markRead(Long id) {
        User user = getCurrentUser();
        Notification notification = notificationRepository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Notification with id:" + id + " does not exist."
                ));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteMine(Long id) {
        User user = getCurrentUser();
        Notification notification = notificationRepository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Notification with id:" + id + " does not exist."
                ));
        notificationRepository.delete(notification);
    }

    @Transactional
    public void clearMine() {
        User user = getCurrentUser();
        notificationRepository.deleteByUserId(user.getId());
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
