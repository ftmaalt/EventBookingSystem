package com.project.bookngo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSEService {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);

    public SseEmitter subscribe(Long userId) {
        logger.info("SSE subscription requested for user ID: {}", userId);
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        return emitter;
    }

    public void sendNotification(Long userId, String eventName, Object payload) {
        SseEmitter emitter= emitters.get(userId);
        if (emitter == null)
            return;
        try {
            logger.info("Sending SSE notification '{}' to user ID: {}", eventName, userId);
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException e) {
            logger.error("Failed to send SSE notification to user ID: {}", userId, e);
            emitters.remove(userId, emitter);
        }
    }
}