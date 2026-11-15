package com.bookngo.notificationservice.controller;

import com.bookngo.notificationservice.dto.response.GenericMessageResponse;
import com.bookngo.notificationservice.dto.response.NotificationResponse;
import com.bookngo.notificationservice.service.CurrentUserService;
import com.bookngo.notificationservice.service.NotificationService;
import com.bookngo.notificationservice.service.SSEService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SSEService sseService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, SSEService sseService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.sseService = sseService;
        this.currentUserService = currentUserService;
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
        return sseService.subscribe(currentUserService.getCurrentUser().id());
    }
}
