package com.bookngo.notificationservice.service;

import com.bookngo.notificationservice.dto.response.NotificationResponse;
import com.bookngo.notificationservice.event.NotificationCreatedEvent;
import com.bookngo.notificationservice.exception.InformationNotFoundException;
import com.bookngo.notificationservice.model.Notification;
import com.bookngo.notificationservice.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                               ApplicationEventPublisher eventPublisher,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.currentUserService = currentUserService;
    }

    /** Stores a notification for a user; the SSE push happens after this transaction commits. */
    @Transactional
    public NotificationResponse create(Long userId, String type, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);
        NotificationResponse response = toResponse(notificationRepository.save(notification));
        eventPublisher.publishEvent(new NotificationCreatedEvent(userId, type, response));
        return response;
    }

    public List<NotificationResponse> getMine() {
        Long userId = currentUserService.getCurrentUser().id();
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void markAllRead() {
        Long userId = currentUserService.getCurrentUser().id();
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = findMine(id);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteMine(Long id) {
        notificationRepository.delete(findMine(id));
    }

    @Transactional
    public void clearMine() {
        notificationRepository.deleteByUserId(currentUserService.getCurrentUser().id());
    }

    private Notification findMine(Long id) {
        Long userId = currentUserService.getCurrentUser().id();
        return notificationRepository.findByIdAndUserId(id, userId).orElseThrow(() ->
                new InformationNotFoundException("Notification with id:" + id + " does not exist."));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
