
package com.bookngo.bookingservice.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UnSafeCapacityRaceDemoTest {

    @Test
    void demonstratesRaceConditionWithoutLocking()
            throws Exception {

        AtomicInteger availableSeats = new AtomicInteger(1);
        AtomicInteger confirmedBookings = new AtomicInteger(0);

        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> unsafeBooking = () -> {
            int observedSeats = availableSeats.get();

            bothRead.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start timed out");
            }

            if (observedSeats > 0) {
                confirmedBookings.incrementAndGet();
                availableSeats.set(observedSeats - 1);
            }

            return null;
        };

        try {
            Future<Void> customerA = executor.submit(unsafeBooking);
            Future<Void> customerB = executor.submit(unsafeBooking);

            assertTrue(bothRead.await(5, TimeUnit.SECONDS));
            release.countDown();

            customerA.get(5, TimeUnit.SECONDS);
            customerB.get(5, TimeUnit.SECONDS);

            assertEquals(2, confirmedBookings.get(), "both unsafe bookings were confirmed");
            assertEquals(0, availableSeats.get(), "one seat was sold twice");

        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
