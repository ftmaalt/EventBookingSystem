package com.bookngo.authservice.event;

import com.bookngo.authservice.model.ProviderApplication;
import com.bookngo.authservice.model.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An account domain event, published to the Kafka topic "account-events".
 * recipientUserId / recipientEmail name the person the event concerns (for PROVIDER_APPLICATION_SUBMITTED,
 * one event is published per admin who has to review it). Fields that do not apply to a type are null.
 * eventId is unique per event and lets consumers detect a redelivered message.
 */
public record AccountEvent(
        String eventId,
        AccountEventType eventType,
        Instant occurredAt,
        Long recipientUserId,
        String recipientEmail,
        String token,
        String businessName,
        String contactName,
        String city,
        String proposedActivities,
        String reviewNote
) {
    public AccountEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(recipientUserId, "recipientUserId is required");
    }

    // e-mail verification token or  reset password token
    public static AccountEvent withToken(AccountEventType type, User user, String token) {
        return new AccountEvent(newId(), type, Instant.now(), user.getId(), user.getEmail(), token,
                null, null, null, null, null);
    }

// admin receive the application submitted notification --> provider receives the response, either rejection or approval
    public static AccountEvent forApplication(AccountEventType type, User recipient, ProviderApplication application) {
        return new AccountEvent(newId(), type, Instant.now(), recipient.getId(), recipient.getEmail(), null,
                application.getBusinessName(), application.getContactName(), application.getCity(),
                application.getProposedActivities(), application.getReviewNote());
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
