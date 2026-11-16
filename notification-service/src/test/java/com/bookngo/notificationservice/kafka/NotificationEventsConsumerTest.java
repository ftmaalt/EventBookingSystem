package com.bookngo.notificationservice.kafka;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;
import com.bookngo.notificationservice.model.ProcessedEvent;
import com.bookngo.notificationservice.model.enums.NotificationType;
import com.bookngo.notificationservice.repository.ProcessedEventRepository;
import com.bookngo.notificationservice.service.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationEventsConsumerTest {

    private final IdempotentEventHandler handler = mock(IdempotentEventHandler.class);
    private final NotificationEventsConsumer consumer = new NotificationEventsConsumer(handler);

    @Test
    void bookingConfirmedIsMappedToABookingNotification() {
        BookingEvent event = new BookingEvent("evt-1", "BOOKING_CONFIRMED", Instant.now(), 10L, 5L, "user@test.com",
                3L, 7L, "Kayaking", LocalDateTime.of(2026, 12, 1, 9, 30), 2, "INDIVIDUAL", new BigDecimal("30.000"));

        consumer.onBookingEvent(event, "booking-events", 0, 42L);

        ArgumentCaptor<NotificationEventRequest> captor = ArgumentCaptor.forClass(NotificationEventRequest.class);
        verify(handler).handleOnce(eq("evt-1"), eq("booking-events"), captor.capture());
        NotificationEventRequest request = captor.getValue();
        assertThat(request.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(request.userId()).isEqualTo(5L);
        assertThat(request.email()).isEqualTo("user@test.com");
        assertThat(request.bookingId()).isEqualTo(10L);
        assertThat(request.activityTitle()).isEqualTo("Kayaking");
        assertThat(request.startTime()).isEqualTo("2026-12-01 09:30");
        assertThat(request.totalPrice()).isEqualByComparingTo("30");
    }

    @Test
    void userRegisteredIsMappedToAVerificationEmail() {
        AccountEvent event = new AccountEvent("evt-2", "USER_REGISTERED", Instant.now(), 5L, "new@test.com",
                "token-123", null, null, null, null, null);

        consumer.onAccountEvent(event, "account-events", 1, 0L);

        ArgumentCaptor<NotificationEventRequest> captor = ArgumentCaptor.forClass(NotificationEventRequest.class);
        verify(handler).handleOnce(eq("evt-2"), eq("account-events"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.EMAIL_VERIFICATION);
        assertThat(captor.getValue().token()).isEqualTo("token-123");
        assertThat(captor.getValue().email()).isEqualTo("new@test.com");
    }

    @Test
    void providerRejectionCarriesTheReviewNote() {
        AccountEvent event = new AccountEvent("evt-3", "PROVIDER_APPLICATION_REJECTED", Instant.now(), 8L, "p@test.com",
                null, "Sea Tours", "Ali", "Manama", "Diving", "Missing licence");

        consumer.onAccountEvent(event, "account-events", 0, 1L);

        ArgumentCaptor<NotificationEventRequest> captor = ArgumentCaptor.forClass(NotificationEventRequest.class);
        verify(handler).handleOnce(eq("evt-3"), eq("account-events"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.PROVIDER_APPLICATION_REJECTED);
        assertThat(captor.getValue().reason()).isEqualTo("Missing licence");
        assertThat(captor.getValue().businessName()).isEqualTo("Sea Tours");
    }

    @Test
    void unknownEventTypesAreIgnoredNotFailed() {
        BookingEvent event = new BookingEvent("evt-4", "BOOKING_COMPLETED", Instant.now(), 10L, 5L, "u@test.com",
                3L, 7L, "Kayaking", null, 1, null, null);

        consumer.onBookingEvent(event, "booking-events", 0, 2L);

        verifyNoInteractions(handler);
    }

    @Test
    void duplicateEventIsDispatchedOnlyOnce() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        IdempotentEventHandler realHandler = new IdempotentEventHandler(repository, dispatcher);
        NotificationEventRequest request = mock(NotificationEventRequest.class);

        when(repository.existsById("evt-5")).thenReturn(false, true);
        realHandler.handleOnce("evt-5", "booking-events", request);
        realHandler.handleOnce("evt-5", "booking-events", request); // redelivery

        verify(dispatcher, times(1)).dispatch(request);
        verify(repository, times(1)).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    void eventWithoutIdIsRejected() {
        IdempotentEventHandler realHandler = new IdempotentEventHandler(
                mock(ProcessedEventRepository.class), mock(NotificationDispatcher.class));

        assertThatThrownBy(() -> realHandler.handleOnce(null, "booking-events", mock(NotificationEventRequest.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
