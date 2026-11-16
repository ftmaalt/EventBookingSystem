package com.bookngo.complianceservice.dto.request;

import com.bookngo.complianceservice.model.enums.ViolationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportViolationRequest {
    @NotNull
    private ViolationType type;
    @NotNull
    private Boolean excused;
    private String note;
    private Long bookingId;
    private Long sessionId;
    @NotNull
    private Long userId;
}
