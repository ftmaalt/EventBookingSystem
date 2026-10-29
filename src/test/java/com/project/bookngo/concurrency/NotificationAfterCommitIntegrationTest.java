package com.project.bookngo.concurrency;

import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.response.NotificationResponse;
import com.project.bookngo.repository.NotificationRepository;
import com.project.bookngo.repository.UsersRepository;
import com.project.bookngo.service.NotificationService;
import com.project.bookngo.service.SSEService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * Integration tests for transaction-safe real-time SSE notifications.
 * Verifies that SSE events are dispatched only after a successful database
 * commit and are suppressed when a transaction rolls back.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationAfterCommitIntegrationTest {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SSEService sseService;

    private User customer;

    @BeforeEach
    void setUp() {

        customer = new User();

        customer.setFullName("SSE Test Customer");
        customer.setEmail(
                "sse-" + UUID.randomUUID() + "@test.com");
        customer.setPasswordHash("integration-test-only");
        customer.setRole(UserRole.USER);
        customer.setStatus(UserStatus.ACTIVE);
        customer.setMustChangePassword(false);
        customer.setStrikeCount((short) 0);
        customer.setConsecutiveViolations((short) 0);

        customer = usersRepository.saveAndFlush(customer);
    }
    /**
     * Requirement: Deliver real-time SSE notifications only after successful commits.
     * Given a notification created within a transaction, when the transaction
     * commits, then the notification remains persisted and SSE broadcasts it
     * exactly once. No SSE message is sent before the commit.
     */
    @Test
    void committedNotificationSendsSseAfterCommit() {

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {

            notificationService.create(customer, "booking-confirmed", "Your booking is confirmed.");

            verifyNoInteractions(sseService);
        });

        verify(sseService, timeout(5000).times(1)).sendNotification(
                        eq(customer.getId()),
                        eq("booking-confirmed"),
                        any(NotificationResponse.class));

        assertEquals(1, notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(customer.getId()).size());
    }
    /**
     * Requirement: Prevent real-time notifications for rolled-back transactions.
     * Given a notification created within a transaction, when the transaction
     * rolls back, then no notification is persisted and no SSE event is sent.
     * Verifies transaction-safe asynchronous notification handling.
     */
    @Test
    void rolledBackNotificationDoesNotSendSse() {

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {

            notificationService.create(customer, "booking-confirmed", "This notification must be rolled back.");

            status.setRollbackOnly();
        });
        assertTrue(notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(customer.getId()).isEmpty());

        verify(sseService, after(1000).never()).sendNotification(eq(customer.getId()), eq("booking-confirmed"), any(NotificationResponse.class));
    }
}
