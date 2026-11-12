package com.bookngo.bookingservice.dto.response;

import com.bookngo.bookingservice.model.enums.BookingStatus;
import com.bookngo.bookingservice.model.enums.BookingType;
import com.bookngo.bookingservice.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class BookingResponse {
    private Long id;
    private Long sessionId;
    private Integer participants;
    private BookingType bookingType;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}