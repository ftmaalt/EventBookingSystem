package com.bookngo.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps one open SSE connection per user (in memory, so it works for a single instance). */
@Service
public class SSEService {

    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));
        return emitter;
    }

    public void sendNotification(Long userId, String eventName, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException e) {
            logger.warn("SSE connection for user {} is gone, removing it.", userId);
            emitters.remove(userId, emitter);
        }
    }
}
