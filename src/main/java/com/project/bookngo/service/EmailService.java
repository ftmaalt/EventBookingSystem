package com.project.bookngo.service;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.ProviderApplication;
import com.project.bookngo.model.enums.SessionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        logger.info("Sending verification email to: {}", toEmail);        SimpleMailMessage verificationMessage = new SimpleMailMessage();
        verificationMessage.setTo(toEmail);
        verificationMessage.setSubject("Verify your BooknGo email");
        verificationMessage.setText(
                "Hello," +
                        "\nHere is your verification link:\n" + baseUrl + "/api/auth/verify?token=" + token+
                        "\n\nThis email verification link will expire after 24 hours. If you did not create an account on BooknGo, no further action is required.\n" +
                        "\n" +
                        "Regards,\n" +
                        "BooknGo Team");

        mailSender.send(verificationMessage);
    }

    public void sendPasswordResetEmail(String toEmail, String token){
        logger.info("Sending password reset email to: {}", toEmail);        SimpleMailMessage passwordResetMessage= new SimpleMailMessage();
        passwordResetMessage.setTo(toEmail);
        passwordResetMessage.setSubject("Reset your BooknGo account password");
        passwordResetMessage.setText("Hello," +
                "\nYou are receiving this email because we received a password reset request for your account." + "Use this link to reset it: "  + baseUrl + "/api/auth/reset-password?token=" + token +
                "\n\nThis password reset link will expire in 6 minutes.If you did not request a password reset, no further action is required.\n" +
                "\n" +
                "Regards,\n" +
                "BooknGo Team");
        mailSender.send(passwordResetMessage);
    }

    public void sendBookingVerifiedEmail(String toEmail, Bookings booking ){
        logger.info("Sending booking confirmation email for booking ID: {} to: {}", booking.getBookingId(), toEmail);
        SimpleMailMessage bookingConfirmedMessage= new SimpleMailMessage();
        bookingConfirmedMessage.setTo(toEmail);
        bookingConfirmedMessage.setSubject("Booking Confirmed For "+booking.getSession()+ "BID#"+booking.getBookingId());
        String emailText = String.format(
                "Hello %s,\n\n" +
                        "Great news! Your booking has been successfully confirmed.\n\n" +
                        "==========================================\n" +
                        "             BOOKING DETAILS              \n" +
                        "==========================================\n" +
                        "Booking Reference : #%d\n" +
                        "Session           : %s\n" +
                        "Date & Time       : %s\n" +
                        "Booking Type      : %s\n" +
                        "Participants      : %d\n" +
                        "Booking Status    : %s\n\n" +
                        "------------------------------------------\n" +
                        "             PAYMENT SUMMARY              \n" +
                        "------------------------------------------\n" +
                        "Subtotal          : $%.3f\n" +
                        "Discount          : %s\n" +
                        "Total Amount      : $%.3f\n" +
                        "Payment Status    : %s\n" +
                        "==========================================\n\n" +
                        "Please make sure to arrive on time for your session.\n\n" +
                        "Thank you for choosing BookNGo!\n\n" +
                        "Best regards,\n" +
                        "The BookNGo Team",

                booking.getUser() != null ? booking.getUser().getFullName() : "Customer",

                booking.getBookingId(),
                booking.getSession() != null ? booking.getSession().getActivity().getTitle() : "N/A",
                booking.getSession() != null ? booking.getSession().getStartTime() : "N/A",
                booking.getBookingType(),
                booking.getParticipants(),
                booking.getStatus(),


                booking.getSubtotal(),
                booking.getDiscountPercent() != null ? booking.getDiscountPercent() + "%" : "0%",
                booking.getTotalPrice(),
                booking.getPaymentStatus()
        );

        bookingConfirmedMessage.setText(emailText);
        mailSender.send(bookingConfirmedMessage);
    }
    public void sendBookingReminders(String toEmail, Bookings booking, String timeFrameLabel) {
        logger.info("Sending booking reminder for booking ID: {} to: {}", booking != null ? booking.getBookingId() : null, toEmail);
        if (booking == null || booking.getSession() == null) return;

        String activityTitle = (booking.getSession().getActivity() != null)
                ? booking.getSession().getActivity().getTitle() : "N/A";
        String userName = (booking.getUser() != null && booking.getUser().getFullName() != null)
                ? booking.getUser().getFullName() : "Customer";

        SimpleMailMessage reminderMessage = new SimpleMailMessage();
        reminderMessage.setTo(toEmail);
        reminderMessage.setSubject("Booking Reminder - Session Starts in " + timeFrameLabel);

        String emailText = String.format(
                "Hello %s,\n\nThis is a reminder that your booking for session: %s starts in %s.\n\n" +
                        "==========================================\n" +
                        "             BOOKING DETAILS              \n" +
                        "==========================================\n" +
                        "Booking Reference : #%d\n" +
                        "Session           : %s\n" +
                        "Date & Time       : %s\n" +
                        "Booking Type      : %s\n" +
                        "Participants      : %d\n" +
                        "Booking Status    : %s\n\n" +
                        "Please make sure to arrive on time for your session.\n\n" +
                        "Thank you for choosing BookNGo!\n\nBest regards,\nThe BookNGo Team",
                userName, activityTitle, timeFrameLabel, booking.getBookingId(), activityTitle,
                booking.getSession().getStartTime(), booking.getBookingType(),
                booking.getParticipants(), booking.getStatus()
        );

        reminderMessage.setText(emailText);
        mailSender.send(reminderMessage);
    }
    public void sendSessionCancelledEmail(String toEmail, Bookings booking) {
        logger.info("Sending cancellation email for booking ID: {} to: {}", booking != null ? booking.getBookingId() : null,toEmail);

        if (booking == null || booking.getSession() == null) {
            return;
        }

        String userName = (booking.getUser() != null && booking.getUser().getFullName() != null)
                ? booking.getUser().getFullName()
                : "Customer";

        String activityTitle = (booking.getSession().getActivity() != null)
                ? booking.getSession().getActivity().getTitle()
                : "N/A";

        String cancellationReason = (booking.getCancellationReason() != null && !booking.getCancellationReason().isBlank())
                ? booking.getCancellationReason()
                : "Unforeseen circumstances";

        SimpleMailMessage cancellationMessage = new SimpleMailMessage();
        cancellationMessage.setTo(toEmail);
        cancellationMessage.setSubject("Important Update: Session Cancelled - Ref #" + booking.getSession().getId());

        String emailText = String.format(
                "Hello %s,\n\n" +
                        "We regret to inform you that your upcoming booking for \"%s\" has been CANCELLED.\n\n" +
                        "==========================================\n" +
                        "          CANCELLED BOOKING DETAILS       \n" +
                        "==========================================\n" +
                        "Booking Reference : #%d\n" +
                        "Session           : %s\n" +
                        "Scheduled Time    : %s\n" +
                        "Participants      : %d\n" +
                        "Reason            : %s\n\n" +
                        "------------------------------------------\n" +
                        "             REFUND & NEXT STEPS          \n" +
                        "------------------------------------------\n" +
                        "Total Amount      : $%.3f\n" +
                        "Payment Status    : %s\n\n" +
                        "A full refund will be processed back to your original payment method within 3–5 business days.\n\n" +
                        "We apologize for any inconvenience this may cause. If you have questions or need help rescheduling, please reply to this email.\n\n" +
                        "Best regards,\n" +
                        "The BookNGo Team",

                userName,
                activityTitle,
                booking.getBookingId(),
                activityTitle,
                booking.getSession().getStartTime() != null ? booking.getSession().getStartTime() : "N/A",
                booking.getParticipants(),
                cancellationReason,
                booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO,
                booking.getPaymentStatus()
        );

        cancellationMessage.setText(emailText);
        mailSender.send(cancellationMessage);
    }
    public void sendNewApplicationNotification(String adminEmail, ProviderApplication application) {
        logger.info("Sending provider application notification to admin: {}", adminEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("New Provider Application: " + application.getBusinessName());
        message.setText(String.format(
                "Hello,\n\nA new provider application has been submitted and is awaiting review.\n\n" +
                        "Business Name     : %s\n" +
                        "Contact Name      : %s\n" +
                        "City              : %s\n" +
                        "Proposed Activities: %s\n\n" +
                        "Please log in to review it.\n\n" +
                        "Regards,\nBookNGo System",
                application.getBusinessName(),
                application.getContactName(),
                application.getCity(),
                application.getProposedActivities()
        ));
        mailSender.send(message);
    }
}

