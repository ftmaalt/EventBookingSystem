package com.project.bookngo.model.request;

import com.project.bookngo.model.Category;
import com.project.bookngo.model.Location;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(min = 1)
    private BigDecimal price_per_person;
    @Size(min = 10)
    private Integer duration_minutes;
    private Long category_id;
    private Long id;
}
