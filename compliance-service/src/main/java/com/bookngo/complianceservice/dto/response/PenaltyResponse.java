package com.bookngo.complianceservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PenaltyResponse(Long id, Short strikeNumber, BigDecimal amount, String status,
                              LocalDateTime createdAt, LocalDateTime paidAt) {}
