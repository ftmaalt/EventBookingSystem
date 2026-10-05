package com.project.bookngo.model.request;

import com.project.bookngo.model.enums.BookingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequest {
    @NotNull
    private Long sessionId;

    @NotNull
    @Min(1)
    private Integer participants;

    @NotNull
    private BookingType bookingType;
}