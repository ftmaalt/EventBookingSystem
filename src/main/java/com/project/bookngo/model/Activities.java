package com.project.bookngo.model;

import com.project.bookngo.enums.ActivityStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "activities")
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    private String description;

    @Column(precision = 10, scale = 3)
    @NotBlank
    private BigDecimal pricePerPerson;

    @Column
    @NotBlank
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private ActivityStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;


//    --- Relationship Mapping ---


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "activity")
    private List<Sessions> sessionsList;
}
