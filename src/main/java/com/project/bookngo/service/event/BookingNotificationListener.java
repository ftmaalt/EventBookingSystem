package com.project.bookngo.service.event;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.service.EmailService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@RequiredArgsConstructor
@Component
public class BookingNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final BookingsRepository bookingsRepository;
    private final EmailService emailService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingNotification(BookingNotificationEvent event) {

        logger.info("Processing {} for booking {} on thread {}", event.type(), event.bookingId(), Thread.currentThread().getName());
        try {
            Bookings booking = bookingsRepository
                    .findForNotification(event.bookingId())
                    .orElseThrow(() -> new IllegalStateException("Booking not found: " + event.bookingId()));

            String email = booking.getUser().getEmail();

            switch (event.type()) {
                case BOOKING_CONFIRMED, WAITLIST_ALLOCATED -> {
                    if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        logger.info(
                                "Skipping confirmation for booking {}: status is {}",
                                booking.getBookingId(),
                                booking.getStatus());
                        return;
                    }
                    emailService.sendBookingVerifiedEmail(
                            email, booking
                    );
                }
                case BOOKING_CANCELLED -> {
                    emailService.sendSessionCancelledEmail(
                            email, booking
                    );
                }
            }
            logger.info(
                    "Notification {} completed for booking {}", event.type(), event.bookingId());

        } catch (Exception ex) {
            logger.error("Failed to process notification {} for booking {}", event.type(), event.bookingId(), ex);
        }
    }
}
