package com.project.bookngo.model;

import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.BookingType;
import com.project.bookngo.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    private Integer participants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType bookingType;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal subtotal;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 255)
    private String cancellationReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 20)
    private String lastReminderStage;

    @Column(length = 150)
    private String createdBy;

    @Column(length = 150)
    private String updatedBy;

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
