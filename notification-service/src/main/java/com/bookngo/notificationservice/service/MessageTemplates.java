package com.bookngo.notificationservice.service;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;

import java.math.RoundingMode;

/** user-facing text */
final class MessageTemplates {

    record EmailContent(String subject, String body) {}

    private static final String SIGN_OFF = "\n\nBest regards,\nThe BookNGo Team";

    private MessageTemplates() {}

    static String inApp(NotificationEventRequest r) {
        String title = or(r.activityTitle(), "your activity");
        return switch (r.type()) {
            case BOOKING_CONFIRMED -> "Your booking #" + r.bookingId() + " for " + title + " has been confirmed.";
            case WAITLIST_ALLOCATED -> "A seat opened up! Your waitlist spot for " + title
                    + " is now a confirmed booking (#" + r.bookingId() + ").";
            case BOOKING_CANCELLED -> "Your booking #" + r.bookingId() + " for " + title + " was cancelled.";
            case BOOKING_REMINDER -> "Reminder: " + title + " starts in " + or(r.timeFrame(), "a short while") + ".";
            case SESSION_CANCELLED -> "The session for " + title + " has been cancelled. Reason: "
                    + or(r.reason(), "unforeseen circumstances") + ".";
            case NEW_PROVIDER_APPLICATION -> "New provider application from " + or(r.businessName(), "a provider")
                    + " is waiting for review.";
            case PROVIDER_APPLICATION_APPROVED -> "Your provider application for " + or(r.businessName(), "your business")
                    + " was approved.";
            case PROVIDER_APPLICATION_REJECTED -> "Your provider application for " + or(r.businessName(), "your business")
                    + " was rejected." + (r.reason() == null || r.reason().isBlank() ? "" : " Note: " + r.reason());
            default -> throw new IllegalArgumentException(r.type() + " is not an in-app notification.");
        };
    }

    static EmailContent email(NotificationEventRequest r, String frontendUrl) {
        String title = or(r.activityTitle(), "your activity");
        return switch (r.type()) {
            case BOOKING_CONFIRMED -> new EmailContent("Booking Confirmed BID#" + r.bookingId(),
                    "Hello,\n\nGreat news! Your booking has been confirmed.\n\n" + bookingDetails(r, title)
                            + "\nPlease arrive on time for your session." + SIGN_OFF);
            case WAITLIST_ALLOCATED -> new EmailContent("A seat opened up - Booking Confirmed BID#" + r.bookingId(),
                    "Hello,\n\nA seat became available and you were moved from the waitlist into a confirmed booking.\n\n"
                            + bookingDetails(r, title) + "\nPlease arrive on time for your session." + SIGN_OFF);
            case BOOKING_CANCELLED -> new EmailContent("Booking Cancelled BID#" + r.bookingId(),
                    "Hello,\n\nYour booking has been cancelled.\n\n" + bookingDetails(r, title) + SIGN_OFF);
            case BOOKING_REMINDER -> new EmailContent("Booking Reminder - Session Starts in " + or(r.timeFrame(), "a short while"),
                    "Hello,\n\nThis is a reminder that your booking for " + title + " starts in "
                            + or(r.timeFrame(), "a short while") + ".\n\n" + bookingDetails(r, title)
                            + "\nPlease arrive on time for your session." + SIGN_OFF);
            case SESSION_CANCELLED -> new EmailContent("Important Update: Session Cancelled - Ref #" + r.bookingId(),
                    "Hello,\n\nWe regret to inform you that your upcoming booking for \"" + title
                            + "\" has been CANCELLED.\n\n" + bookingDetails(r, title) + "Reason            : "
                            + or(r.reason(), "Unforeseen circumstances") + "\n\nWe apologize for any inconvenience." + SIGN_OFF);
            case EMAIL_VERIFICATION -> new EmailContent("Verify your BooknGo email",
                    "Hello,\n\nHere is your verification link:\n" + frontendUrl + "/verify-email?token=" + r.token()
                            + "\n\nThis link expires after 24 hours. If you did not create an account on BooknGo, no action is required."
                            + SIGN_OFF);
            case PASSWORD_RESET -> new EmailContent("Reset your BooknGo account password",
                    "Hello,\n\nWe received a password reset request for your account. Use this link to reset it:\n"
                            + frontendUrl + "/reset-password?token=" + r.token()
                            + "\n\nThis link expires in 6 minutes. If you did not request a reset, no action is required."
                            + SIGN_OFF);
            case NEW_PROVIDER_APPLICATION -> new EmailContent("New Provider Application: " + or(r.businessName(), "unknown"),
                    "Hello,\n\nA new provider application is awaiting review.\n\n"
                            + "Business Name      : " + or(r.businessName(), "N/A") + "\n"
                            + "Contact Name       : " + or(r.contactName(), "N/A") + "\n"
                            + "City               : " + or(r.city(), "N/A") + "\n"
                            + "Proposed Activities: " + or(r.proposedActivities(), "N/A")
                            + "\n\nPlease log in to review it.\n\nRegards,\nBookNGo System");
            case PROVIDER_APPLICATION_APPROVED -> new EmailContent("Your BookNGo provider application was approved",
                    "Hello,\n\nYour provider application for " + or(r.businessName(), "your business")
                            + " was approved. You can now log in and create activities." + SIGN_OFF);
            case PROVIDER_APPLICATION_REJECTED -> new EmailContent("Your BookNGo provider application was rejected",
                    "Hello,\n\nYour provider application for " + or(r.businessName(), "your business")
                            + " was rejected.\n\nNote: " + or(r.reason(), "No note was given.") + SIGN_OFF);
        };
    }

    private static String bookingDetails(NotificationEventRequest r, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("Booking Reference : #").append(or(r.bookingId(), "N/A")).append('\n');
        sb.append("Session           : ").append(title).append('\n');
        sb.append("Date & Time       : ").append(or(r.startTime(), "N/A")).append('\n');
        if (r.bookingType() != null) {
            sb.append("Booking Type      : ").append(r.bookingType()).append('\n');
        }
        if (r.participants() != null) {
            sb.append("Participants      : ").append(r.participants()).append('\n');
        }
        if (r.totalPrice() != null) {
            sb.append("Total Amount      : $").append(r.totalPrice().setScale(3, RoundingMode.HALF_UP).toPlainString()).append('\n');
        }
        sb.append("==========================================\n");
        return sb.toString();
    }

    private static String or(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
