package com.project.bookngo.controller;

import com.project.bookngo.model.User;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.model.response.NotificationResponse;
import com.project.bookngo.repository.UsersRepository;
import com.project.bookngo.service.NotificationService;
import com.project.bookngo.service.SSEService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private SSEService sseService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersRepository usersRepository;

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    @GetMapping
    public List<NotificationResponse> getMine() {
        return notificationService.getMine();
    }

    @PatchMapping("/read-all")
    public GenericMessageResponse markAllRead() {
        notificationService.markAllRead();
        return new GenericMessageResponse("Notifications marked as read.");
    }

    @PatchMapping("/{id}/read")
    public GenericMessageResponse markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return new GenericMessageResponse("Notification marked as read.");
    }

    @DeleteMapping("/{id}")
    public GenericMessageResponse deleteMine(@PathVariable Long id) {
        notificationService.deleteMine(id);
        return new GenericMessageResponse("Notification deleted.");
    }

    @DeleteMapping
    public GenericMessageResponse clearMine() {
        notificationService.clearMine();
        return new GenericMessageResponse("Notifications cleared.");
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        User user = getCurrentUser();
        logger.info("SSE subscription requested through controller for user ID: {}", user.getId());
        return sseService.subscribe(user.getId());
    }
}
