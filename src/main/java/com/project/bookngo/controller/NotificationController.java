package com.project.bookngo.controller;

import com.project.bookngo.model.User;
import com.project.bookngo.repository.UsersRepository;
import com.project.bookngo.service.SSEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private SSEService sseService;
    @Autowired
    private UsersRepository usersRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        System.out.println("Calling subscribe==>");
        User user = getCurrentUser();
        return sseService.subscribe(user.getId());
    }
}
