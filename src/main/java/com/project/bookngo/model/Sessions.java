package com.project.bookngo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.bookngo.enums.SessionStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "sessions")
public class Sessions {
    @Id
    @NotBlank
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private LocalDateTime startTime;

    @Column
    @NotBlank
    private LocalDateTime endTime;

    @Column
    @NotBlank
    private Integer capacity;

    @Column
    @NotBlank
    private Integer spotsLeft;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private SessionStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    @NotBlank
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

//    --- Relationship Mapping ---

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activities activity;

    @OneToMany(mappedBy = "session")
    private List<Bookings> bookings;


}
