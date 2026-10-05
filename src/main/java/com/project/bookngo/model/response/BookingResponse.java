package com.project.bookngo.model.response;

import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.BookingType;
import com.project.bookngo.model.enums.PaymentStatus;
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