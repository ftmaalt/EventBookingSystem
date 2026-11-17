package com.bookngo.notificationservice;

import com.bookngo.notificationservice.dto.response.NotificationResponse;
import com.bookngo.notificationservice.repository.NotificationRepository;
import com.bookngo.notificationservice.service.NotificationService;
import com.bookngo.notificationservice.service.SSEService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SSE notifications are pushed only after the notification is committed,
 * and never for a rolled-back transaction.
 */
@SpringBootTest
class NotificationAfterCommitIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SSEService sseService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = ThreadLocalRandom.current().nextLong(1_000_000_000L, 2_000_000_000L);
    }

    @Test
    void committedNotificationSendsSseAfterCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.create(userId, "booking-confirmed", "Your booking is confirmed.");
            verifyNoInteractions(sseService);
        });

        verify(sseService, timeout(5000).times(1))
                .sendNotification(eq(userId), eq("booking-confirmed"), any(NotificationResponse.class));
        assertEquals(1, notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId).size());
    }

    @Test
    void rolledBackNotificationDoesNotSendSse() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.create(userId, "booking-confirmed", "This notification must be rolled back.");
            status.setRollbackOnly();
        });

        assertTrue(notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId).isEmpty());
        verify(sseService, after(1000).never())
                .sendNotification(eq(userId), eq("booking-confirmed"), any(NotificationResponse.class));
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> notificationRepository.deleteByUserId(userId));
    }
}
