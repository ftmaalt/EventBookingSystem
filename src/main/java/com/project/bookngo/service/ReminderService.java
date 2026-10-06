package com.project.bookngo.service;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.repository.BookingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {
    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 15 * 60 * 1000)// checks for time every 15 minutes
    public void sendUpcomingBookingReminders(){
        LocalDateTime now= LocalDateTime.now();
        //finds any bookings coming in 24 hours
        LocalDateTime window= now.plusHours(24);

        List<Bookings> upcoming = bookingsRepository.findByStatusAndSession_StartTimeBetween(BookingStatus.CONFIRMED, now, window);

        for (Bookings booking :upcoming){
            emailService.sendBookingReminders(booking.getUser().getEmail(), booking);
        }

    }
}
