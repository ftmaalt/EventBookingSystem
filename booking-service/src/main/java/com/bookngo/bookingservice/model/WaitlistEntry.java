package com.project.bookngo.model;

import com.project.bookngo.model.enums.WaitListStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "waitlist")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long entryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitListStatus status = WaitListStatus.WAITING;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime joinedAt;


//    --- Relationship Mapping ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Sessions session;

}
