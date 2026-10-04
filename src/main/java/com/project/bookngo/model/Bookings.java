package com.project.bookngo.model;

import com.project.bookngo.enums.BookingStatus;
import com.project.bookngo.enums.BookingType;
import com.project.bookngo.enums.PaymentStatus;
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
@Table(name = "bookings")
public class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private Integer participants;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private BookingType bookingType;

    @Column(precision = 10, scale = 3)
    @NotBlank
    private BigDecimal  subtotal;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(precision = 10, scale = 3)
    @NotBlank
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private PaymentStatus paymentStatus;

    @Column(length = 255)
    private String cancellationReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;


//    --- Relationship Mapping ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Sessions session;

    @OneToMany(mappedBy = "booking")
    private List<Violations> violations;
}
