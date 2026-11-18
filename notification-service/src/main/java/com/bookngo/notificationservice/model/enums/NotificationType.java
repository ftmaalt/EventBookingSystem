package com.bookngo.notificationservice.model.enums;

public enum NotificationType {
    BOOKING_CONFIRMED("booking-confirmed", true),
    WAITLIST_ALLOCATED("waitlist-allocated", true),
    BOOKING_CANCELLED("booking-cancelled", true),
    SESSION_CANCELLED("session-cancelled", true),
    EMAIL_VERIFICATION("email-verification", false),
    PASSWORD_RESET("password-reset", false),
    NEW_PROVIDER_APPLICATION("new-provider-application", true),
    PROVIDER_APPLICATION_APPROVED("provider-application-approved", true),
    PROVIDER_APPLICATION_REJECTED("provider-application-rejected", true);

    private final String code;
    private final boolean inApp;

    NotificationType(String code, boolean inApp) {
        this.code = code;
        this.inApp = inApp;
    }
    public String code() {
        return code;
    }
    public boolean isInApp() {
        return inApp;
    }
}
