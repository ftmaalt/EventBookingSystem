package com.bookngo.authservice;

import com.bookngo.authservice.config.RateLimitConfig;
import com.bookngo.authservice.exception.RateLimitExceededException;
import com.bookngo.authservice.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        RateLimitConfig config = new RateLimitConfig();
        config.setCapacity(5);
        config.setLeakRateMs(60_000);
        rateLimitService = new RateLimitService(config);
    }

    @Test
    void allowsUpToCapacityThenRejects() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkRateLimit("10.0.0.1", "user@test.com");
        }

        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit("10.0.0.1", "user@test.com"));
    }

    @Test
    void limitsAnEmailAcrossDifferentIps() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkRateLimit("10.0.0." + i, "user@test.com");
        }

        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit("10.0.0.99", "user@test.com"));
    }

    @Test
    void treatsEmailCaseAndSpacesAsTheSameAccount() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkRateLimit("10.0.0." + i, "User@Test.com ");
        }

        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit("10.0.0.99", "user@test.com"));
    }

    @Test
    void otherAccountsAreNotAffected() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkRateLimit("10.0.0." + i, "user@test.com");
        }

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit("10.0.1.1", "other@test.com"));
    }

    @Test
    void concurrentAttemptsNeverExceedCapacity() throws Exception {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                String ip = "10.1.0." + i;
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        rateLimitService.checkRateLimit(ip, "user@test.com");
                        allowed.incrementAndGet();
                    } catch (RateLimitExceededException e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(5, allowed.get());
        assertEquals(15, rejected.get());
    }
}
