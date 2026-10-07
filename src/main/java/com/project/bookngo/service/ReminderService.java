package com.project.bookngo.service;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.repository.BookingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {
    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 10 * 60 * 1000)// checks for time every 10 minutes
    public void sendUpcomingBookingReminders(){
        LocalDateTime now= LocalDateTime.now();
        //finds any bookings coming in 24 hours
        LocalDateTime window= now.plusHours(24);

        List<Bookings> upcoming = bookingsRepository.findByStatusAndSession_StartTimeBetween(BookingStatus.CONFIRMED, now, window);

        for (Bookings booking :upcoming) {
            long minutesToBooking = Duration.between(now, booking.getSession().getStartTime()).toMinutes();
            String timeFrame = reminderFrame(minutesToBooking);

            emailService.sendBookingReminders(booking.getUser().getEmail(), booking, timeFrame);
            booking.setLastReminderStage(timeFrame);
            bookingsRepository.save(booking);
        }
    }
    public String reminderFrame(long minutesToBooking){
        if (minutesToBooking<= 0) {
            return null;
        }
        if (minutesToBooking <= 30){
            return "30 Minutes";
        }
        if (minutesToBooking <= 60){
            return "1 Hour";
        }
        if (minutesToBooking <= 360){
            return "6 Hours";
        }
        if (minutesToBooking <= 1440){
            return "24 Hours";
        }
        return null;
    }
}
