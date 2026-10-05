package com.project.bookngo.model.request;

import com.project.bookngo.model.Category;
import com.project.bookngo.model.Location;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ActivityRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal pricePerPerson;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long locationId;
}

