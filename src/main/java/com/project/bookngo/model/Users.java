package com.project.bookngo.model;

import com.project.bookngo.enums.UserRole;
import com.project.bookngo.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(length = 255)
    private String profilePicturePath;

    @Column(nullable = false)
    private Boolean mustChangePassword;

    @Column(nullable = false)
    private Short strikeCount;

    @Column(nullable = false)
    private Short consecutiveViolations;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    //    --- Relationship Mapping ---

    @OneToMany(mappedBy = "penalties")
    private List<Penalties> penalties;

    @OneToMany(mappedBy = "bookings")
    private List<Bookings> bookings;

    @OneToMany(mappedBy = "violations")
    private List<Violations> violations;
}
