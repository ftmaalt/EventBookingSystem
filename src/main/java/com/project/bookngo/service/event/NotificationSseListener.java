package com.project.bookngo.service.event;

import com.project.bookngo.service.SSEService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class NotificationSseListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSseListener.class);

    private final SSEService sseService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event){
        logger.info("Broadcasting SSE '{}' to user {} on thread {}", event.type(), event.userId(), Thread.currentThread().getName());

        try {
            sseService.sendNotification(event.userId(), event.type(), event.notification());

        } catch (Exception ex) {
            logger.error("Failed to broadcast SSE '{}' to user {}", event.type(), event.userId(), ex);
        }
    }
}



}
