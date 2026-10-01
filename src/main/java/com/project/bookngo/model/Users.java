package com.project.bookngo.model;

import com.project.bookngo.enums.UserRole;
import com.project.bookngo.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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


    @Column(length = 100)
    @NotBlank
    private String fullName;

    @Column(unique = true, length = 150)
    @NotBlank
    private String email;

    @Column(length = 255)
    @NotBlank
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private UserStatus status;

    @Column(length = 255)
    private String profilePicturePath;

    @Column
    private Boolean mustChangePassword;

    @Column
    private Short strikeCount;

    @Column
    private Short consecutiveViolations;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    //    --- Relationship Mapping ---

    @OneToMany(mappedBy = "user")
    private List<Penalties> penalties;

    @OneToMany(mappedBy = "user")
    private List<Bookings> bookings;

    @OneToMany(mappedBy = "user")
    private List<Violations> violations;
}
